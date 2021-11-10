package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.impl.GraphQLBatch;
import io.vertx.ext.web.handler.graphql.impl.GraphQLInput;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.github.ixtf.api.guice.ApiModule.GRAPHQL_ADDRESS;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static java.util.Optional.ofNullable;

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
        try {
            final var graphQLInput = GraphQLInput.decode(reply.body());
            if (graphQLInput instanceof final GraphQLQuery query) {
                final var deliveryOptions = new DeliveryOptions();
                final var result = handleQuery(reply, query);
                final var jsonObject = new JsonObject(result.toSpecification());
                if (J.nonEmpty(result.getErrors())) {
                    deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "400");
                }
                reply.reply(jsonObject.encode(), deliveryOptions);
            } else if (graphQLInput instanceof final GraphQLBatch batch) {
                final var deliveryOptions = new DeliveryOptions();
                final var jsonArray = new JsonArray();
                StreamSupport.stream(batch.spliterator(), false).forEach(it -> {
                    final var result = handleQuery(reply, it);
                    jsonArray.add(result.toSpecification());
                    if (J.nonEmpty(result.getErrors())) {
                        deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "400");
                    }
                });
                reply.reply(jsonArray.toBuffer(), deliveryOptions);
            } else {
                reply.fail(400, "no GraphQLInput");
            }
        } catch (Throwable e) {
            final var errorMessage = e.getMessage();
            reply.fail(400, errorMessage);
            log.error("", e);
            spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true).log(errorMessage).finish());
        } finally {
            spanOpt.ifPresent(Span::finish);
        }
    }

    private ExecutionResult handleQuery(Message<Buffer> reply, GraphQLQuery query) {
        return graphQL.execute(builder -> {
            builder.query(query.getQuery());
            ofNullable(query.getOperationName()).filter(J::nonBlank).ifPresent(builder::operationName);
            ofNullable(query.getVariables()).ifPresent(builder::variables);
            builder.graphQLContext(ctx -> reply.headers().forEach(entry -> ctx.of(entry.getKey(), entry.getValue())));
            return builder;
        });
    }

    private Collection<ExecutionResult> handleBatch(Message<Buffer> reply, GraphQLBatch batch) {
        return StreamSupport.stream(batch.spliterator(), false)
                .map(it -> handleQuery(reply, it))
                .collect(Collectors.toUnmodifiableList());
    }
}
