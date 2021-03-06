package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
import com.github.ixtf.api.ApiAction;
import com.github.ixtf.api.ApiResponse;
import com.github.ixtf.exception.JError;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

import static com.github.ixtf.api.ApiResponse.bodyMono;
import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ReplyHandler implements Handler<Message<Object>> {
    private final Object instance;
    private final Method method;
    private final String address;
    @Getter
    private final String operationName;
    private final Logger log;
    @Getter
    @Inject
    private Optional<Tracer> tracerOpt;

    private ReplyHandler(Method method) {
        this.method = method;

        final var declaringClass = method.getDeclaringClass();
        instance = getInstance(declaringClass);

        final var annotation = method.getAnnotation(ApiAction.class);
        final var service = annotation.service();
        final var action = annotation.action();
        address = String.join(":", service, action);

        operationName = String.join(":", instance.getClass().getName(), method.getName());
        log = LoggerFactory.getLogger(instance.getClass());
    }

    public static Future<Void> consumer(Method method) {
        return Future.future(p -> {
            final var handler = new ReplyHandler(method);
            injectMembers(handler);
            getInstance(Vertx.class).eventBus().consumer(handler.address, handler).completionHandler(p);
        });
    }

    public static Future<Void> localConsumer(Method method) {
        return Future.future(p -> {
            final var handler = new ReplyHandler(method);
            injectMembers(handler);
            getInstance(Vertx.class).eventBus().localConsumer(handler.address, handler).completionHandler(p);
        });
    }

    @Override
    public void handle(Message<Object> reply) {
        final var ctx = new VertxContext(reply, tracerOpt, operationName);
        final var spanOpt = ctx.spanOpt();
        Mono.fromCallable(() -> bodyMono(method.invoke(instance, ctx)))
                .flatMap(Function.identity())
                .subscribe(it -> {
                    if (it instanceof ApiResponse) {
                        reply(reply, (ApiResponse) it, spanOpt);
                    } else {
                        reply(reply, it, new DeliveryOptions(), spanOpt);
                    }
                }, e -> fail(reply, e, spanOpt));
    }

    private void reply(Message<Object> reply, ApiResponse apiResponse, Optional<Span> spanOpt) {
        final var deliveryOptions = new DeliveryOptions();
        apiResponse.getHeaders().forEach((k, v) -> deliveryOptions.addHeader(k, v));
        deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "" + apiResponse.getStatus());
        apiResponse.bodyMono().subscribe(it -> reply(reply, it, deliveryOptions, spanOpt), e -> fail(reply, e, spanOpt));
    }

    private void reply(Message<Object> reply, Object o, DeliveryOptions deliveryOptions, Optional<Span> spanOpt) {
        if (o instanceof String) {
            final var v = (String) o;
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
