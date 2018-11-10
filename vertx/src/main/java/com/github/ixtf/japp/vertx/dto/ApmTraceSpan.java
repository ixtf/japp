package com.github.ixtf.japp.vertx.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.ixtf.japp.core.J;
import com.google.common.collect.Maps;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.RoutingContext;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.github.ixtf.japp.core.Constant.MAPPER;

/**
 * @author jzb 2018-11-10
 */
@Data
public class ApmTraceSpan implements Serializable {
    private String traceId;
    private String type;
    private int parentId = 0;
    private int spanId;
    private long start = System.currentTimeMillis();
    private String name;
    private String address;
    private ObjectNode request = MAPPER.createObjectNode();
    private ObjectNode meta = MAPPER.createObjectNode();
    private ObjectNode receive = MAPPER.createObjectNode();
    private ObjectNode response = MAPPER.createObjectNode();
    private boolean error;
    private String errorMessage;

    public static ApmTraceSpan decode(Message reply) {
        return decode(reply.headers().get("apmTrace"));
    }

    @SneakyThrows
    public static ApmTraceSpan decode(String s) {
        return J.isBlank(s) ? null : MAPPER.readValue(s, ApmTraceSpan.class);
    }

    public ApmTraceSpan next() {
        final ApmTraceSpan result = new ApmTraceSpan();
        result.setTraceId(traceId);
        result.setParentId(spanId);
        result.setSpanId(spanId + 1);
        return result;
    }

    public void requestBy(RoutingContext rc) {
        final JsonNode pathParams = MAPPER.convertValue(rc.pathParams(), JsonNode.class);
        request.set("pathParams", pathParams);

        final Map<String, List<String>> queryParamsMap = Maps.newConcurrentMap();
        final MultiMap queryParamsMultiMap = rc.queryParams();
        queryParamsMultiMap.names().stream().forEach(name -> {
            final List<String> strings = rc.queryParam(name);
            queryParamsMap.put(name, strings);
        });
        final JsonNode queryParams = MAPPER.convertValue(queryParamsMap, JsonNode.class);
        request.set("queryParams", queryParams);

        request.put("body", rc.getBodyAsString());
    }

    @SneakyThrows
    public String encode() {
        return MAPPER.writeValueAsString(this);
    }

    public void addMeta(String key, String value) {
        meta.put(key, value);
    }

    public void fillData(DeliveryOptions deliveryOptions) {
        deliveryOptions.addHeader("apmTrace", this.encode());
    }
}
