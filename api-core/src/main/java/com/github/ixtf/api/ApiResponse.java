package com.github.ixtf.api;

import com.google.common.collect.Maps;
import io.netty.util.AsciiString;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.github.ixtf.Constant.MAPPER;

@Accessors(chain = true)
public class ApiResponse {
    @Getter
    @Setter
    private int status = 200;
    @Getter
    private final Map<String, String> headers = Maps.newConcurrentMap();
    @Setter
    private Object body;

    public static Mono<?> bodyMono(Object o) {
        if (o == null) {
            return Mono.just(Buffer.buffer());
        }
        if (o instanceof String || o instanceof byte[] || o instanceof ApiResponse) {
            return Mono.just(o);
        }
        if (o instanceof JsonObject v) {
            return Mono.just(v.toBuffer());
        }
        if (o instanceof JsonArray v) {
            return Mono.just(v.toBuffer());
        }
        if (o instanceof CompletionStage v) {
            return Mono.fromCompletionStage(v).flatMap(ApiResponse::bodyMono).defaultIfEmpty(Buffer.buffer());
        }
        if (o instanceof Mono v) {
            return v.flatMap(ApiResponse::bodyMono).defaultIfEmpty(Buffer.buffer());
        }
        if (o instanceof Flux v) {
            return v.collectList().map(it -> new JsonArray((List) it).toBuffer());
        }
        return Mono.fromCallable(() -> MAPPER.writeValueAsBytes(o));
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

    public Mono<?> bodyMono() {
        return bodyMono(body);
    }
}
