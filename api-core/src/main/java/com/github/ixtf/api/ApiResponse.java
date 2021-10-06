package com.github.ixtf.api;

import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import io.vertx.core.eventbus.DeliveryOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Accessors(chain = true)
public class ApiResponse {
    @Getter
    @Setter
    private int status = 200;
    @Getter
    private final Map<String, String> headers = Maps.newConcurrentMap();
    @Getter
    @Setter
    private Object body;

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

    public DeliveryOptions ensure(DeliveryOptions deliveryOptions) {
        getHeaders().forEach(deliveryOptions::addHeader);
        deliveryOptions.addHeader(HttpResponseStatus.class.getName(), "" + getStatus());
        return deliveryOptions;
    }
}
