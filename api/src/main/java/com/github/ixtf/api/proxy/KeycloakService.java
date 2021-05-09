package com.github.ixtf.api.proxy;

import com.github.ixtf.api.proxy.impl.KeycloakServiceImpl;
import com.google.inject.ImplementedBy;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
@ImplementedBy(KeycloakServiceImpl.class)
public interface KeycloakService {
    String ADDRESS = "__:ApiModule:com.github.ixtf.api.proxy.KeycloakService__";

    void find(String id, Handler<AsyncResult<JsonObject>> resultHandler);
}
