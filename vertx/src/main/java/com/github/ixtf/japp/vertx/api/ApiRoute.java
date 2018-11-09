package com.github.ixtf.japp.vertx.api;

import com.github.ixtf.japp.core.J;
import com.github.ixtf.japp.vertx.Jvertx;
import com.github.ixtf.japp.vertx.annotations.Address;
import com.github.ixtf.japp.vertx.spi.ApiGateway;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
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

    protected ApiRoute(String path, Method method) {
        this.path = path;
        this.method = method;
        argsExtr = new ArgsExtr(method);
        address = address();
    }

    private String address() {
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
        final Single<JsonArray> args$;
        final Vertx vertx = rc.vertx();
        if (argsExtr.isHasPrincipal()) {
            args$ = apiGateway.rxPrincipal(rc)
                    .map(J::defaultString)
                    .map(principal -> argsExtr.extr(rc, principal));
        } else {
            args$ = Single.fromCallable(() -> argsExtr.extr(rc, null));
        }
        args$.flatMap(it -> vertx.eventBus().<String>rxSend(address, it)).subscribe(it -> {
            final HttpServerResponse response = rc.response();
            final String body = it.body();
            if (body == null) {
                response.end();
            } else {
                response.end(body);
            }
        }, ex -> {
            log.error("", ex);
            rc.fail(ex);
        });
    }

    public Completable rxConsume(Vertx vertx) {
        final Object proxy = apiGateway.getProxy(method.getDeclaringClass());
        return vertx.eventBus().<JsonArray>consumer(address, reply -> Single.fromCallable(() -> {
                    final Object[] args = argsExtr.extr(reply);
                    return method.invoke(proxy, args);
                }).flatMapCompletable(it -> retHandler(it, reply))
                        .doOnError(ex -> {
                            log.error("", ex);
                            reply.fail(400, ex.getMessage());
                        })
                        .subscribe()
        ).rxCompletionHandler();
    }

    private Completable retHandler(Object result, Message reply) {
        if (result == null) {
            reply.reply(null);
            return Completable.complete();
        }
        if (result instanceof Single) {
            return ((Single) result).flatMapCompletable(it -> retHandler(it, reply));
        }
        if (result instanceof Flowable) {
            return ((Flowable) result).toList().flatMapCompletable(it -> retHandler(it, reply));
        }
        if (result instanceof Completable) {
            return ((Completable) result).doOnComplete(() -> reply.reply(null));
        }
        return Completable.fromAction(() -> {
            if (result instanceof String) {
                reply.reply(result);
            } else {
                reply.reply(MAPPER.writeValueAsString(result));
            }
        });
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
