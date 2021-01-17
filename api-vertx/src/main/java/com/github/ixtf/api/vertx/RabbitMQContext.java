package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
import com.github.ixtf.api.ApiContext;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Envelope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQMessage;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class RabbitMQContext implements ApiContext {
    private final RabbitMQClient client;
    private final RabbitMQMessage message;
    private final Optional<Tracer> tracerOpt;
    private final String operationName;
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final Map<String, String> headers = _headers();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final byte[] body = _body();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final Optional<Span> spanOpt = _spanOpt();

    public RabbitMQContext(RabbitMQClient client, RabbitMQMessage message, Optional<Tracer> tracerOpt, String operationName) {
        this.client = client;
        this.message = message;
        this.tracerOpt = tracerOpt;
        this.operationName = operationName;
    }

    private Map<String, String> _headers() {
        final var builder = ImmutableMap.<String, String>builder();
        ofNullable(message.properties())
                .map(BasicProperties::getHeaders)
                .map(Map::entrySet)
                .stream()
                .flatMap(Collection::stream)
                .forEach(entry -> builder.put(entry.getKey(), entry.getValue().toString()));
        return builder.build();
    }

    @Override
    public Map<String, String> headers() {
        return getHeaders();
    }

    private byte[] _body() {
        return message.body().getBytes();
    }

    @Override
    public byte[] body() {
        return getBody();
    }

    @Override
    public Optional<Tracer> tracerOpt() {
        return tracerOpt;
    }

    private Optional<Span> _spanOpt() {
        return Util.spanOpt(tracerOpt(), operationName, headers()).map(span -> {
            final var dest = Stream.of(
                    ofNullable(message.envelope()).map(Envelope::getExchange),
                    ofNullable(message.envelope()).map(Envelope::getRoutingKey)
            ).flatMap(Optional::stream).filter(J::nonBlank).collect(Collectors.joining(":"));
            return span.setTag(Tags.MESSAGE_BUS_DESTINATION, dest);
        });
    }

    @Override
    public Optional<Span> spanOpt() {
        return getSpanOpt();
    }
}
