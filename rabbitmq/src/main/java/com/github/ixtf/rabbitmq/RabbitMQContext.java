package com.github.ixtf.rabbitmq;

import com.github.ixtf.J;
import com.github.ixtf.api.ApiContext;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Envelope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.SendOptions;
import reactor.rabbitmq.Sender;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Slf4j
public class RabbitMQContext implements ApiContext {
    private final Sender sender;
    private final SendOptions sendOptions;
    private final AcknowledgableDelivery delivery;
    private final Optional<Tracer> tracerOpt;
    private final String operationName;
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final Map<String, String> headers = _headers();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final byte[] body = _body();
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final Optional<Span> spanOpt = _spanOpt();

    public RabbitMQContext(Sender sender, SendOptions sendOptions, AcknowledgableDelivery delivery, Optional<Tracer> tracerOpt, String operationName) {
        this.sender = sender;
        this.sendOptions = sendOptions;
        this.delivery = delivery;
        this.tracerOpt = tracerOpt;
        this.operationName = operationName;
    }

    public void ack() {
        delivery.ack();
    }

    public void nack() {
        delivery.nack(false);
    }

    private Map<String, String> _headers() {
        final var builder = ImmutableMap.<String, String>builder();
        ofNullable(delivery.getProperties())
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
        return delivery.getBody();
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
                    ofNullable(delivery.getEnvelope()).map(Envelope::getExchange),
                    ofNullable(delivery.getEnvelope()).map(Envelope::getRoutingKey)
            ).flatMap(Optional::stream).filter(J::nonBlank).collect(Collectors.joining(":"));
            return span.setTag(Tags.MESSAGE_BUS_DESTINATION, dest);
        });
    }

    @Override
    public Optional<Span> spanOpt() {
        return getSpanOpt();
    }
}
