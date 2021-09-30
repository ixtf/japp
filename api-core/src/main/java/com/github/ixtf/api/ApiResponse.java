package com.github.ixtf.api;

import com.google.common.collect.Maps;
import io.netty.util.AsciiString;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Accessors(chain = true)
public class ApiResponse {
    @Getter
    @Setter
    private int status = 200;
    @Getter
    private final Map<String, String> headers = Maps.newConcurrentMap();
    @Setter
    private Object body;

    public static CompletionStage<?> bodyFuture(Object o) {
        if (o instanceof final Mono<?> v) {
            return bodyFuture(v.toFuture());
        }
        if (o instanceof final Flux<?> v) {
            return bodyFuture(v.collectList().map(JsonArray::new));
        }
        if (o instanceof final Future<?> future) {
            return future.toCompletionStage();
        }
        if (o instanceof final JsonObject v) {
            return CompletableFuture.supplyAsync(v::toBuffer);
        }
        if (o instanceof final JsonArray v) {
            return CompletableFuture.supplyAsync(v::toBuffer);
        }
        if (o instanceof final CompletionStage<?> v) {
            return v.thenCompose(ApiResponse::bodyFuture);
        }
        return CompletableFuture.completedStage(o);
    }

    public CompletionStage<?> bodyFuture() {
        return bodyFuture(body);
    }

    public ApiResponse putHeaders(final String key, final String value) {
        headers.put(key, value);
        return this;
    }

    public ApiResponse putHeaders(final AsciiString key, final AsciiString value) {
        return putHeaders(key.toString(), value.toString());
    }

    public ApiResponse putHeaders(final AsciiString key, final String value) {
        return putHeaders(key.toString(), value);
    }
}
