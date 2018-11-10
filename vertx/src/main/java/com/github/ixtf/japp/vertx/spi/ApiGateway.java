package com.github.ixtf.japp.vertx.spi;

import com.github.ixtf.japp.vertx.dto.ApmTraceSpan;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

/**
 * @author jzb 2018-11-01
 */
public interface ApiGateway {

    default String addressPrefix() {
        return "japp-vertx";
    }

    Completable rxMount(Router router);

    Completable rxConsume(Vertx vertx);

    Single<String> rxPrincipal(RoutingContext rc);

    <T> T getProxy(Class<T> clazz);

    default void submitApmSpan(ApmTraceSpan apmSpan) {
    }
}
