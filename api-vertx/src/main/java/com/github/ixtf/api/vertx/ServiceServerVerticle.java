package com.github.ixtf.api.vertx;

import com.github.ixtf.api.ApiAction;
import com.github.ixtf.api.ApiResponse;
import com.github.ixtf.exception.JError;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static com.github.ixtf.Constant.MAPPER;
import static com.github.ixtf.api.ApiResponse.bodyFuture;
import static com.github.ixtf.api.guice.ApiModule.ACTIONS;
import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
public class ServiceServerVerticle extends AbstractVerticle {
    @Named(ACTIONS)
    @Inject
    private Collection<Method> methods;
    @Inject
    private Optional<Tracer> tracerOpt;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);
        CompositeFuture.all(methods.stream().map(method -> {
            final var handler = new ReplyHandler(method);
            final var consumer = vertx.eventBus().consumer(handler.address).handler(handler);
            return Future.<Void>future(p -> consumer.completionHandler(p)).onFailure(e -> log.error(handler.address, e));
        }).collect(toUnmodifiableList())).<Void>mapEmpty().onComplete(startPromise);
    }

    private class ReplyHandler implements Handler<Message<Object>> {
        private final Object instance;
        private final Method method;
        private final String address;
        private final Logger instanceLog;

        private ReplyHandler(Method method) {
            this.method = method;

            final var declaringClass = method.getDeclaringClass();
            instanceLog = LoggerFactory.getLogger(declaringClass);
            instance = getInstance(declaringClass);

            final var annotation = method.getAnnotation(ApiAction.class);
            final var service = annotation.service();
            final var action = annotation.action();
            address = String.join(":", service, action);
        }

        @Override
        public void handle(Message<Object> reply) {
            final var ctx = new VertxContext(reply, tracerOpt, address);
            final var spanOpt = ctx.spanOpt();
            Mono.fromCallable(() -> bodyFuture(method.invoke(instance, ctx)))
                    .doFinally(__ -> spanOpt.ifPresent(Span::finish))
                    .subscribe(it -> onSuccess(reply, it, new DeliveryOptions(), spanOpt), e -> onFail(reply, e, spanOpt));
        }

        private void onSuccess(Message<Object> reply, CompletionStage<?> completionStage, DeliveryOptions deliveryOptions, Optional<Span> spanOpt) {
            completionStage.whenComplete((v, e) -> {
                if (e != null) {
                    onFail(reply, e, spanOpt);
                } else {
                    onSuccess(reply, v, deliveryOptions, spanOpt);
                }
            });
        }

        private void onSuccess(Message<Object> reply, Object o, DeliveryOptions deliveryOptions, Optional<Span> spanOpt) {
            if (o == null || o instanceof String || o instanceof Buffer || o instanceof byte[]) {
                reply.reply(o, deliveryOptions);
            } else if (o instanceof ApiResponse) {
                final var apiResponse = (ApiResponse) o;
                apiResponse.getHeaders().forEach((k, v) -> deliveryOptions.addHeader(k, v));
                deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "" + apiResponse.getStatus());
                onSuccess(reply, apiResponse.bodyFuture(), deliveryOptions, spanOpt);
            } else {
                Mono.fromCallable(() -> MAPPER.writeValueAsBytes(o)).subscribe(it -> onSuccess(reply, it, deliveryOptions, spanOpt), e -> onFail(reply, e, spanOpt));
            }
        }

        private void onFail(Message<Object> reply, Throwable e, Optional<Span> spanOpt) {
            spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true));
            if (e.getCause() != null) {
                onFail(reply, e.getCause(), spanOpt);
            } else if (e instanceof JError || e instanceof ConstraintViolationException) {
                reply.fail(400, e.getMessage());
            } else {
                reply.fail(501, e.getMessage());
                instanceLog.error(address, e);
                spanOpt.ifPresent(span -> span.log(e.getMessage()));
            }
        }
    }

}
