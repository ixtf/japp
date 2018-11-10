package com.github.ixtf.japp.vertx.api;

import com.github.ixtf.japp.codec.Jcodec;
import com.github.ixtf.japp.core.J;
import com.github.ixtf.japp.vertx.Jvertx;
import com.github.ixtf.japp.vertx.annotations.Address;
import com.github.ixtf.japp.vertx.annotations.ApmTrace;
import com.github.ixtf.japp.vertx.dto.ApmTraceSpan;
import com.github.ixtf.japp.vertx.spi.ApiGateway;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static com.github.ixtf.japp.core.Constant.MAPPER;

/**
 * @author jzb 2018-10-31
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public abstract class ApiRoute {
    protected final ApiGateway apiGateway = Jvertx.apiGateway();
    @ToString.Include
    @Getter
    protected final String path;
    @ToString.Include
    @Getter
    protected final String address;
    @Getter
    protected final Method method;
    protected final ArgsExtr argsExtr;
    protected final ApmTrace apmTrace;

    protected ApiRoute(String path, Method method) {
        this.path = path;
        this.method = method;
        argsExtr = new ArgsExtr(method);
        address = _address();
        apmTrace = _apmTrace();
    }

    private ApmTrace _apmTrace() {
        return Optional.ofNullable(method.getAnnotation(ApmTrace.class))
                .orElseGet(() -> method.getDeclaringClass().getAnnotation(ApmTrace.class));
    }

    private String _address() {
        final Address annotation = method.getAnnotation(Address.class);
        if (annotation != null) {
            return annotation.value();
        }
        return String.join(":", apiGateway.addressPrefix(), addressPrefix(), path);
    }

    protected abstract String addressPrefix();

    protected abstract Route route(Router router);

    public Completable rxMount(Router router) {
        return Completable.fromAction(() -> {
            final Route route = route(router);
            Arrays.stream(produces()).forEach(route::produces);
            Arrays.stream(consumes()).forEach(route::consumes);
            route.handler(this::handler);
        });
    }

    private void handler(RoutingContext rc) {
        argsExtr.rxToMessage(rc).flatMap(message -> {
            final DeliveryOptions deliveryOptions = new DeliveryOptions();
            if (apmTrace != null) {
                final ApmTraceSpan apmTraceSpan = new ApmTraceSpan();
                apmTraceSpan.setTraceId(Jcodec.uuid());
                apmTraceSpan.setType(apmTrace.type());
                apmTraceSpan.setAddress(address);
                apmTraceSpan.setSpanId(1000);
                apmTraceSpan.requestBy(rc);
                apmTraceSpan.addMeta("invoke_args", message.encode());
                apiGateway.submitApmSpan(apmTraceSpan);
                apmTraceSpan.fillData(deliveryOptions);
            }
            return rc.vertx().eventBus().<String>rxSend(address, message, deliveryOptions);
        }).subscribe(message -> {
            final HttpServerResponse response = rc.response();
            final String body = message.body();
            if (J.isBlank(body)) {
                response.end();
            } else {
                response.end(body);
            }
        }, rc::fail);
    }

    public Completable rxConsume(Vertx vertx) {
        final Object proxy = apiGateway.getProxy(method.getDeclaringClass());
        return vertx.eventBus().<JsonArray>consumer(address, reply -> Single.fromCallable(() -> {
                    final Object[] args = argsExtr.extr(reply);
                    return method.invoke(proxy, args);
                }).flatMapCompletable(it -> replyHandler(it, reply))
                        .doOnError(ex -> errorReplyHandler(reply, ex))
                        .subscribe()
        ).rxCompletionHandler();
    }

    private Completable replyHandler(Object result, Message<JsonArray> reply) {
        if (result == null) {
            reply.reply(null);
            return Completable.complete();
        }
        if (result instanceof Single) {
            return ((Single) result).flatMapCompletable(it -> replyHandler(it, reply));
        }
        if (result instanceof Flowable) {
            return ((Flowable) result).toList().flatMapCompletable(it -> replyHandler(it, reply));
        }
        if (result instanceof Completable) {
            return ((Completable) result).doOnComplete(() -> reply.reply(null));
        }
        return Completable.fromAction(() -> {
            final String replyString = result instanceof String ? (String) result : MAPPER.writeValueAsString(result);
            final DeliveryOptions deliveryOptions = new DeliveryOptions();
            if (apmTrace != null) {
                final ApmTraceSpan parentApmTraceSpan = ApmTraceSpan.decode(reply);
                final ApmTraceSpan apmTraceSpan = parentApmTraceSpan.next();
                apmTraceSpan.setType(apmTrace.type());
                apmTraceSpan.setAddress(address);
                apmTraceSpan.getReceive().put("body", reply.body().encode());
                apmTraceSpan.getResponse().put("reply", replyString);
                apiGateway.submitApmSpan(apmTraceSpan);
                apmTraceSpan.fillData(deliveryOptions);
            }
            reply.reply(replyString, deliveryOptions);
        });
    }

    private void errorReplyHandler(Message<JsonArray> reply, Throwable ex) {
        log.error("", ex);
        if (apmTrace != null) {
            final ApmTraceSpan parentApmTraceSpan = ApmTraceSpan.decode(reply);
            final ApmTraceSpan apmTraceSpan = parentApmTraceSpan.next();
            apmTraceSpan.setType(apmTrace.type());
            apmTraceSpan.setAddress(address);
            apmTraceSpan.setError(true);
            apmTraceSpan.setErrorMessage(ex.getMessage());
            apmTraceSpan.setEnd(System.currentTimeMillis());
            apiGateway.submitApmSpan(apmTraceSpan);
        }
        reply.fail(400, ex.getMessage());
    }

    private String[] consumes() {
        Consumes annotation = method.getAnnotation(Consumes.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(Consumes.class);
        }
        return annotation == null ? new String[0] : annotation.value();
    }

    private String[] produces() {
        Produces annotation = method.getAnnotation(Produces.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(Produces.class);
        }
        return annotation == null ? new String[0] : annotation.value();
    }

}
