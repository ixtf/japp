package com.github.ixtf.api.vertx;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.github.ixtf.api.guice.ApiModule.ACTIONS;
import static com.github.ixtf.guice.GuiceModule.injectMembers;

public class ServiceServerVerticle extends AbstractVerticle {
    @Named(ACTIONS)
    @Inject
    private Collection<Method> methods;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);
        CompositeFuture.all(methods.stream()
                .map(ReplyHandler::consumer)
                .collect(Collectors.toUnmodifiableList()))
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

}
