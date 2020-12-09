package com.github.ixtf.api.vertx;

import com.github.ixtf.api.ApiContext;
import com.google.common.collect.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;

import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class VertxContext implements ApiContext {
    private final ReplyHandler handler;
    private final Message reply;
    private final Map<String, String> headers;
    private final Optional<Span> spanOpt;

    public VertxContext(ReplyHandler handler, Message reply) {
        this.handler = handler;
        this.reply = reply;

        final var builder = ImmutableMap.<String, String>builder();
        reply.headers().forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
        headers = builder.build();

        spanOpt = spanOpt(handler.getOperationName());
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public byte[] body() {
        final var body = reply.body();
        if (body instanceof Buffer) {
            final var buffer = (Buffer) body;
            return buffer.getBytes();
        }
        final var s = (String) body;
        return s.getBytes(UTF_8);
    }

    @Override
    public Optional<Tracer> tracerOpt() {
        return handler.getTracerOpt();
    }

    @Override
    public Optional<Span> spanOpt() {
        return spanOpt;
    }
}
