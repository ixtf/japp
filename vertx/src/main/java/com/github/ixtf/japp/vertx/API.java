package com.github.ixtf.japp.vertx;

import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.RoutingContext;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jzb 2018-08-30
 */
public interface API {
    static void handle(RoutingContext rc, String service, DeliveryOptions deliveryOptions) {
        handle(rc, service, new JsonObject(), deliveryOptions);
    }

    static void handle(RoutingContext rc, String service, JsonObject message, DeliveryOptions deliveryOptions) {
        final Vertx vertx = rc.vertx();
        vertx.eventBus().<String>rxSend(service, mergePrincipal(message, rc), deliveryOptions)
                .subscribeOn(RxHelper.scheduler(vertx))
                .subscribe(toObserver(rc));
    }

    static JsonObject mergePrincipal(JsonObject message, RoutingContext rc) {
        final JsonObject result = Optional.ofNullable(message).orElse(new JsonObject());
        return Optional.ofNullable(rc)
                .map(RoutingContext::user)
                .map(User::principal)
                .map(it -> result.put(EB.Body.principal, it))
                .orElse(result);
    }

    static SingleObserver<Message<String>> toObserver(RoutingContext rc) {
        final AtomicBoolean completed = new AtomicBoolean();
        return new SingleObserver<Message<String>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull Message<String> item) {
                if (completed.compareAndSet(false, true)) {
                    rc.response().end(item.body());
                }
            }

            @Override
            public void onError(Throwable error) {
                if (completed.compareAndSet(false, true)) {
                    // todo handle ServiceException
                    rc.fail(error);
                }
            }
        };
    }
}
