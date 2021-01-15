package com.github.ixtf.api.vertx;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;

import java.lang.reflect.Method;
import java.util.Collection;

import static com.github.ixtf.api.guice.ApiModule.ACTIONS;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static java.util.stream.Collectors.toUnmodifiableList;

public class ServiceServerVerticle extends AbstractVerticle {
    @Named(ACTIONS)
    @Inject
    private Collection<Method> methods;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);
        CompositeFuture.all(methods.stream()
                .map(ReplyHandler::consumer)
                .collect(toUnmodifiableList()))
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }

}
