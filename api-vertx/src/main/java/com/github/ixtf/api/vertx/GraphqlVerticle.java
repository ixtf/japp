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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
                handleQuery(reply, query).whenComplete((result,e)->{
                    if (e == null) {
                        final var deliveryOptions = new DeliveryOptions();
                        final var jsonObject = new JsonObject(result.toSpecification());
                        if (J.nonEmpty(result.getErrors())) {
                            deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "400");
                        }
                        reply.reply(jsonObject.toBuffer(), deliveryOptions);
                    } else {
                        onFail(reply,e,spanOpt);
                    }
                });
            } else if (graphQLInput instanceof final GraphQLBatch batch) {
                Flux.fromIterable(batch)
                        .flatMap(query -> {
                            final var result = handleQuery(reply, query);
                            return Mono.fromCompletionStage(result);
                        })
                        .collectList()
                        .subscribe(list -> {
                            final var deliveryOptions = new DeliveryOptions();
                            final var jsonArray = new JsonArray();
                            list.forEach(result -> {
                                jsonArray.add(result.toSpecification());
                                if (J.nonEmpty(result.getErrors())) {
                                    deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "400");
                                }
                            });
                            reply.reply(jsonArray.toBuffer(), deliveryOptions);
                        }, e -> onFail(reply,e,spanOpt));
            } else {
                reply.fail(400, "no GraphQLInput");
            }
        } catch (Throwable e) {
            onFail(reply,e,spanOpt);
        } finally {
            spanOpt.ifPresent(Span::finish);
        }
    }

    private void onFail(Message<Buffer> reply, Throwable e, Optional<Span> spanOpt) {
        final var errorMessage = e.getMessage();
        reply.fail(400, errorMessage);
        log.error("", e);
        spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true).log(errorMessage).finish());
    }

    private CompletableFuture<ExecutionResult> handleQuery(Message<Buffer> reply, GraphQLQuery query) {
        return graphQL.executeAsync(builder -> {
            builder.query(query.getQuery());
            ofNullable(query.getOperationName()).filter(J::nonBlank).ifPresent(builder::operationName);
            ofNullable(query.getVariables()).ifPresent(builder::variables);
            builder.graphQLContext(ctx -> reply.headers().forEach(entry -> ctx.of(entry.getKey(), entry.getValue())));
            return builder;
        });
//        return graphQL.execute(builder -> {
//            builder.query(query.getQuery());
//            ofNullable(query.getOperationName()).filter(J::nonBlank).ifPresent(builder::operationName);
//            ofNullable(query.getVariables()).ifPresent(builder::variables);
//            builder.graphQLContext(ctx -> reply.headers().forEach(entry -> ctx.of(entry.getKey(), entry.getValue())));
//            return builder;
//        });
    }
}
