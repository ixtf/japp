package com.github.ixtf.api.proxy.impl;

import com.github.ixtf.api.proxy.KeycloakService;
import com.google.inject.Singleton;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Singleton
public class KeycloakServiceImpl implements KeycloakService {

    @Override
    public void find(String id, Handler<AsyncResult<JsonObject>> resultHandler) {

    }
}
