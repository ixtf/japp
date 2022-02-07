package com.github.ixtf.api.ws;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.Serializable;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static java.util.Optional.ofNullable;

public class SockJsEvent implements Serializable {
  private final String address;
  private final String type;
  private final Object payload;

  public SockJsEvent(String address, String type, Object payload) {
    this.address = address;
    this.type = type;
    this.payload = payload;
  }

  public SockJsEvent(String address, String type) {
    this(address, type, null);
  }

  public void publish() {
    final var vertx = getInstance(Vertx.class);
    vertx.eventBus().publish(address, message());
  }

  private JsonObject message() {
    final var ret = new JsonObject().put("type", type);
    ofNullable(payload)
        .map(it -> it instanceof String ? it : JsonObject.mapFrom(it))
        .ifPresent(it -> ret.put("payload", it));
    return ret;
  }

  public void send() {
    final var vertx = getInstance(Vertx.class);
    vertx.eventBus().send(address, message());
  }
}
