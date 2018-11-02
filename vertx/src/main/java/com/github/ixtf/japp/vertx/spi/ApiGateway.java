package com.github.ixtf.japp.vertx.spi;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.Router;

/**
 * @author jzb 2018-11-01
 */
public interface ApiGateway {

    default String addressPrefix() {
        return "japp-vertx";
    }

    Completable rxMount(Router router);

    Completable rxConsume(Vertx vertx);

    Single<String> rxPrincipal(Vertx vertx, User user);

    <T> T getProxy(Class<T> clazz);
}
