package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import graphql.ExecutionInput;
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
        Mono.fromCompletionStage(() -> {
            final var graphQLInput = GraphQLInput.decode(reply.body());
            if (graphQLInput instanceof GraphQLBatch) {
                return handleBatch((GraphQLBatch) graphQLInput).map(JsonArray::toBuffer).toCompletionStage();
            } else if (graphQLInput instanceof GraphQLQuery) {
                return handleQuery((GraphQLQuery) graphQLInput).map(JsonObject::toBuffer).toCompletionStage();
            }
            throw new RuntimeException();
        }).subscribe(it -> {
            reply.reply(it);
            spanOpt.ifPresent(Span::finish);
        }, e -> {
            reply.fail(400, e.getMessage());
            log.error("", e);
            spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true).log(e.getMessage()).finish());
        });
    }

    private Future<JsonObject> handleQuery(GraphQLQuery query) {
        final var builder = ExecutionInput.newExecutionInput();
        builder.query(query.getQuery());
        ofNullable(query.getOperationName()).filter(J::nonBlank).ifPresent(builder::operationName);
        ofNullable(query.getVariables()).ifPresent(builder::variables);
        final var completableFuture = graphQL.executeAsync(builder.build());
        return Future.fromCompletionStage(completableFuture, vertx.getOrCreateContext())
                .map(ExecutionResult::toSpecification)
                .map(JsonObject::new);
    }

    private Future<JsonArray> handleBatch(GraphQLBatch batch) {
        return StreamSupport.stream(batch.spliterator(), false)
                .map(q -> (Future) handleQuery(q))
                .collect(collectingAndThen(toList(), CompositeFuture::all))
                .map(CompositeFuture::list)
                .map(JsonArray::new);
    }
}
