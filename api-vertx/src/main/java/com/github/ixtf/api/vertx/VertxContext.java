package com.github.ixtf.api.vertx;

import com.github.ixtf.api.ApiContext;
import com.google.common.collect.ImmutableMap;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.ofNullable;

public class VertxContext implements ApiContext {
    private final ReplyHandler handler;
    private final Message reply;
    private final AtomicReference<Map<String, String>> headers = new AtomicReference();
    private final AtomicReference<Buffer> body = new AtomicReference();
    private final Optional<Span> spanOpt;

    public VertxContext(ReplyHandler handler, Message reply) {
        this.handler = handler;
        this.reply = reply;
        spanOpt = spanOpt(handler.getOperationName());
    }

    @Override
    public Map<String, String> headers() {
        if (headers.get() == null) {
            synchronized (headers) {
                if (headers.get() == null) {
                    final var builder = ImmutableMap.<String, String>builder();
                    reply.headers().forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
                    headers.set(builder.build());
                }
            }
        }
        return headers.get();
    }

    @Override
    public byte[] body() {
        if (body.get() == null) {
            synchronized (body) {
                if (body.get() == null) {
                    body.set(ofNullable(reply.body()).map(it -> {
                        if (it instanceof Buffer) {
                            return (Buffer) it;
                        } else if (it instanceof byte[]) {
                            return Buffer.buffer((byte[]) it);
                        } else {
                            return Buffer.buffer((String) it);
                        }
                    }).orElseGet(Buffer::buffer));
                }
            }
        }
        return body.get().getBytes();
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
