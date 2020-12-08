package com.github.ixtf.api.vertx;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;

import java.lang.reflect.Method;
import java.util.Collection;

import static com.github.ixtf.api.guice.ApiModule.ACTIONS;
import static com.github.ixtf.guice.GuiceModule.injectMembers;

public class ServiceServerVerticle extends AbstractVerticle {
    @Named(ACTIONS)
    @Inject
    private Collection<Method> methods;

    @Override
    public void start() throws Exception {
        injectMembers(this);
        methods.forEach(ReplyHandler::consumer);
    }
}
