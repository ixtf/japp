package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.impl.GraphQLBatch;
import io.vertx.ext.web.handler.graphql.impl.GraphQLInput;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.github.ixtf.api.guice.ApiModule.GRAPHQL_ADDRESS;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

@Slf4j
public class GraphqlVerticle extends AbstractVerticle implements Handler<Message<Buffer>> {
    @Named(GRAPHQL_ADDRESS)
    @Inject
    private String address;
    @Inject
    private GraphQL graphQL;
    @Inject
    private Optional<Tracer> tracerOpt;

    protected Optional<Span> spanOpt(Message<Buffer> reply) {
        final var builder = ImmutableMap.<String, String>builder();
        reply.headers().forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
        final var headers = builder.build();
        return Util.spanOpt(tracerOpt, address, headers);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);
        vertx.eventBus().consumer(address, this).completionHandler(startPromise);
    }

    @Override
    public void handle(Message<Buffer> reply) {
        final var spanOpt = spanOpt(reply);
        final var graphQLInput = GraphQLInput.decode(reply.body());
        if (graphQLInput instanceof GraphQLBatch) {
            handleBatch((GraphQLBatch) graphQLInput).map(JsonArray::toBuffer).onComplete(it -> onComplete(reply, it, spanOpt));
        } else if (graphQLInput instanceof GraphQLQuery) {
            handleQuery((GraphQLQuery) graphQLInput).map(JsonObject::toBuffer).onComplete(it -> onComplete(reply, it, spanOpt));
        } else {
            reply.fail(400, "no GraphQLInput");
        }
    }

    private void onComplete(Message<Buffer> reply, AsyncResult<Buffer> ar, Optional<Span> spanOpt) {
        if (ar.succeeded()) {
            reply.reply(ar.result());
        } else {
            final var cause = ar.cause();
            final var errorMessage = cause.getMessage();
            reply.fail(400, errorMessage);
            log.error("", cause);
            spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true).log(errorMessage).finish());
        }
        spanOpt.ifPresent(Span::finish);
    }

    private Future<JsonObject> handleQuery(GraphQLQuery query) {
        return Future.<ExecutionResult>future(p -> {
            final var executionResult = graphQL.execute(builder -> {
                builder.query(query.getQuery());
                ofNullable(query.getOperationName()).filter(J::nonBlank).ifPresent(builder::operationName);
                ofNullable(query.getVariables()).ifPresent(builder::variables);
                return builder;
            });
            p.complete(executionResult);
        }).map(ExecutionResult::toSpecification).map(JsonObject::new);
    }

    private Future<JsonArray> handleBatch(GraphQLBatch batch) {
        return StreamSupport.stream(batch.spliterator(), false)
                .map(q -> (Future) handleQuery(q))
                .collect(collectingAndThen(toList(), CompositeFuture::all))
                .map(CompositeFuture::list)
                .map(JsonArray::new);
    }
}
