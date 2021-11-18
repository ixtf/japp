package com.github.ixtf.api.vertx;

import com.github.ixtf.api.ApiContext;
import com.github.ixtf.api.ApiResponse;
import com.github.ixtf.exception.JError;
import com.google.inject.Inject;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static com.github.ixtf.Constant.MAPPER;
import static com.github.ixtf.guice.GuiceModule.getInstance;

class ApiReplyHandler implements Handler<Message<Object>> {
    private final String address;
    private final Object instance;
    private final Method method;
    private final Function<ApiContext, Object>[] paramFuns;
    private final Logger instanceLog;
    @Inject
    private Optional<Tracer> tracerOpt;

    ApiReplyHandler(String address, Method method) {
        this.method = method;
        this.address = address;
        paramFuns = paramFuns();

        final var declaringClass = method.getDeclaringClass();
        instanceLog = LoggerFactory.getLogger(declaringClass);
        instance = getInstance(declaringClass);
    }

    private static Function<ApiContext, Object> paramFun(Parameter parameter) {
        final var type = parameter.getType();
        if (ApiContext.class.isAssignableFrom(type)) {
            return (Function) Function.identity();
        }
        if (Principal.class.isAssignableFrom(type)) {
            return ApiContext::principal;
        }
        if (String.class.isAssignableFrom(type)) {
            return ApiContext::bodyAsString;
        }
        if (byte[].class.isAssignableFrom(type)) {
            return ApiContext::body;
        }
        if (JsonObject.class.isAssignableFrom(type)) {
            return ctx -> new JsonObject(Buffer.buffer(ctx.body()));
        }
        if (JsonArray.class.isAssignableFrom(type)) {
            return ctx -> new JsonArray(Buffer.buffer(ctx.body()));
        }
        return ctx -> ctx.command(type);
    }

    private Function<ApiContext, Object>[] paramFuns() {
        final var ret = new Function[method.getParameterCount()];
        if (ret.length > 0) {
            final var parameters = method.getParameters();
            for (var i = 0; i < ret.length; i++) {
                ret[i] = paramFun(parameters[i]);
            }
        }
        return ret;
    }

    @Override
    public void handle(Message<Object> reply) {
        final var ctx = new VertxContext(reply, tracerOpt, address);
        final var spanOpt = ctx.spanOpt();
        try {
            final var objects = Arrays.stream(paramFuns).map(it -> it.apply(ctx)).toArray();
            final var invoke = method.invoke(instance, objects);
            onSuccess(reply, invoke, new DeliveryOptions(), spanOpt);
        } catch (Throwable e) {
            onFail(reply, e, spanOpt);
        }
    }

    private void onSuccess(
            Message<Object> reply, Object o, DeliveryOptions deliveryOptions, Optional<Span> spanOpt) {
        if (o == null || o instanceof String || o instanceof Buffer || o instanceof byte[]) {
            reply.reply(o, deliveryOptions);
            spanOpt.ifPresent(Span::finish);
        } else if (o instanceof final CompletionStage<?> completionStage) {
            completionStage.whenComplete((v, e) -> {
                if (e != null) {
                    onFail(reply, e, spanOpt);
                } else {
                    onSuccess(reply, v, deliveryOptions, spanOpt);
                }
            });
        } else if (o instanceof final Future<?> v) {
            v.onComplete(ar -> {
                if (ar.failed()) {
                    onFail(reply, ar.cause(), spanOpt);
                } else {
                    onSuccess(reply, ar.result(), deliveryOptions, spanOpt);
                }
            });
        } else if (o instanceof final JsonObject v) {
            onSuccess(reply, v.toBuffer(), deliveryOptions, spanOpt);
        } else if (o instanceof final JsonArray v) {
            onSuccess(reply, v.toBuffer(), deliveryOptions, spanOpt);
        } else if (o instanceof final ApiResponse v) {
            onSuccess(reply, v.getBody(), v.ensure(deliveryOptions), spanOpt);
        } else if (o instanceof final Mono<?> v) {
            v.subscribe(it -> onSuccess(reply, it, deliveryOptions, spanOpt), e -> onFail(reply, e, spanOpt));
        } else if (o instanceof final Flux<?> v) {
            onSuccess(reply, v.collectList().map(JsonArray::new), deliveryOptions, spanOpt);
        } else {
            onSuccess(reply, Mono.fromCallable(() -> MAPPER.writeValueAsBytes(o)), deliveryOptions, spanOpt);
        }
    }

    private void onFail(Message<Object> reply, Throwable e, Optional<Span> spanOpt) {
        spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true));
        if (e.getCause() != null) {
            onFail(reply, e.getCause(), spanOpt);
        } else if (e instanceof JError || e instanceof ConstraintViolationException) {
            reply.fail(400, e.getMessage());
            spanOpt.ifPresent(Span::finish);
        } else {
            reply.fail(501, e.getMessage());
            instanceLog.error(address, e);
            spanOpt.ifPresent(span -> span.log(e.getMessage()).finish());
        }
    }
}
