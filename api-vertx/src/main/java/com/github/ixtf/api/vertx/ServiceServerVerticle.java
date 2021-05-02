package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
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
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import static com.github.ixtf.api.ApiResponse.bodyMono;
import static com.github.ixtf.api.guice.ApiModule.ACTIONS;
import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toUnmodifiableList;

public class ServiceServerVerticle extends AbstractVerticle {
    @Named(ACTIONS)
    @Inject
    private Collection<Method> methods;
    @Inject
    private Optional<Tracer> tracerOpt;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);
        CompositeFuture.all(methods.stream()
                .map(ReplyHandler::new)
                .map(ReplyHandler::consumer)
                .collect(toUnmodifiableList()))
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

    private class ReplyHandler implements Handler<Message<Object>> {
        private final Object instance;
        private final Method method;
        private final String address;
        private final Logger log;

        private ReplyHandler(Method method) {
            this.method = method;

            final var declaringClass = method.getDeclaringClass();
            instance = getInstance(declaringClass);

            final var annotation = method.getAnnotation(ApiAction.class);
            final var service = annotation.service();
            final var action = annotation.action();
            address = String.join(":", service, action);

            log = LoggerFactory.getLogger(instance.getClass());
        }

        private Future<Void> consumer() {
            final var consumer = vertx.eventBus().consumer(address).handler(this);
            return Future.<Void>future(p -> consumer.completionHandler(p)).onFailure(e -> log.error("", e));
        }

        @Override
        public void handle(Message<Object> reply) {
            final var ctx = new VertxContext(reply, tracerOpt, address);
            final var spanOpt = ctx.spanOpt();
            Mono.fromCallable(() -> bodyMono(method.invoke(instance, ctx))).subscribe(it -> it.whenComplete((v, e) -> {
                if (e != null) {
                    fail(reply, e, spanOpt);
                } else if (v instanceof ApiResponse apiResponse) {
                    reply(reply, apiResponse, spanOpt);
                } else {
                    reply(reply, v, new DeliveryOptions(), spanOpt);
                }
            }), e -> fail(reply, e, spanOpt));
        }

        private void reply(Message<Object> reply, ApiResponse apiResponse, Optional<Span> spanOpt) {
            final var deliveryOptions = new DeliveryOptions();
            apiResponse.getHeaders().forEach((k, v) -> deliveryOptions.addHeader(k, v));
            deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "" + apiResponse.getStatus());
            apiResponse.bodyMono().whenComplete((v, e) -> {
                if (e != null) {
                    fail(reply, e, spanOpt);
                } else {
                    reply(reply, v, deliveryOptions, spanOpt);
                }
            });
        }

        private void reply(Message<Object> reply, Object o, DeliveryOptions deliveryOptions, Optional<Span> spanOpt) {
            if (o instanceof String v) {
                if (J.isBlank(v)) {
                    reply.reply(null, deliveryOptions);
                } else {
                    deliveryOptions.addHeader(CONTENT_TYPE.toString(), TEXT_PLAIN.toString());
                    reply.reply(v.getBytes(UTF_8), deliveryOptions);
                }
            } else {
                reply.reply(o, deliveryOptions);
            }
            spanOpt.ifPresent(Span::finish);
        }

        private void fail(Message<Object> reply, Throwable e, Optional<Span> spanOpt) {
            if (e.getCause() != null) {
                fail(reply, e.getCause(), spanOpt);
            } else {
                reply.fail(400, e.getMessage());
                spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true).log(e.getMessage()).finish());
                if (!(e instanceof JError || e instanceof ConstraintViolationException)) {
                    log.error(address, e);
                }
            }
        }

    }
}
