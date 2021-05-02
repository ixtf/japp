package com.github.ixtf.api;

import com.google.common.collect.Maps;
import io.netty.util.AsciiString;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;

import static com.github.ixtf.Constant.MAPPER;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

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
            return Mono.just(StringUtils.EMPTY);
        }
        if (o instanceof String || o instanceof Buffer || o instanceof byte[] || o instanceof ApiResponse) {
            return Mono.just(o);
        }
        if (o instanceof JsonObject v) {
            return Mono.just(v.toBuffer());
        }
        if (o instanceof JsonArray v) {
            return Mono.just(v.toBuffer());
        }
        if (o instanceof CompletionStage v) {
            return Mono.fromCompletionStage(v).flatMap(ApiResponse::bodyMono).defaultIfEmpty(StringUtils.EMPTY);
        }
        if (o instanceof Mono v) {
            return v.flatMap(ApiResponse::bodyMono).defaultIfEmpty(StringUtils.EMPTY);
        }
        if (o instanceof Flux v) {
            return v.map(ApiResponse::convertInnerValue).collectList().flatMap(ApiResponse::bodyMono);
        }
        return Mono.fromCallable(() -> MAPPER.writeValueAsBytes(o));
    }

    private static Object convertInnerValue(Object o) {
        if (o instanceof JsonObject v) {
            return v.stream().parallel().collect(toUnmodifiableMap(Entry::getKey, it -> convertInnerValue(it.getValue())));
        }
        if (o instanceof JsonArray v) {
            return v.stream().map(ApiResponse::convertInnerValue).collect(toUnmodifiableList());
        }
        return o;
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
