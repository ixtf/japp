package com.github.ixtf.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;

import static io.vertx.core.VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var deploymentOptions = new DeploymentOptions().setInstances(DEFAULT_EVENT_LOOP_POOL_SIZE);
        vertx.deployVerticle(ApiVerticle.class, deploymentOptions).<Void>mapEmpty().onComplete(startPromise);
    }
}
