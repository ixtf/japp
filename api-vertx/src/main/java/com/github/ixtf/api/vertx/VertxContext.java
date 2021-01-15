package com.github.ixtf.api.vertx;

import com.github.ixtf.api.ApiContext;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class VertxContext implements ApiContext {
    private final ReplyHandler handler;
    private final Message reply;
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final Map<String, String> headers = _headers();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final byte[] body = _body();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final Optional<Span> spanOpt = Util.spanOpt(tracerOpt(), handler.getOperationName(), headers());

    public VertxContext(ReplyHandler handler, Message reply) {
        this.handler = handler;
        this.reply = reply;
    }

    private Map<String, String> _headers() {
        final var builder = ImmutableMap.<String, String>builder();
        reply.headers().forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
        return builder.build();
    }

    @Override
    public Map<String, String> headers() {
        return getHeaders();
    }

    private byte[] _body() {
        return ofNullable(reply.body()).map(it -> {
            if (it instanceof Buffer) {
                return (Buffer) it;
            } else if (it instanceof byte[]) {
                return Buffer.buffer((byte[]) it);
            } else if (it instanceof JsonObject) {
                final var v = (JsonObject) it;
                return Buffer.buffer(v.encode());
            } else if (it instanceof JsonArray) {
                final var v = (JsonArray) it;
                return Buffer.buffer(v.encode());
            } else {
                return Buffer.buffer((String) it);
            }
        }).orElseGet(Buffer::buffer).getBytes();
    }

    @Override
    public byte[] body() {
        return getBody();
    }

    @Override
    public Optional<Tracer> tracerOpt() {
        return handler.getTracerOpt();
    }

    @Override
    public Optional<Span> spanOpt() {
        return getSpanOpt();
    }
}
