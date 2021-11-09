package com.github.ixtf.api.guice;

import com.github.ixtf.J;
import com.github.ixtf.api.ApiContext;
import com.github.ixtf.api.Util;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.ConnectionFactory;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.ixtf.api.guice.ApiModule.CONFIG;
import static com.github.ixtf.api.guice.ApiModule.SERVICE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static reactor.rabbitmq.Utils.singleConnectionMono;

public class RabbitMQModule extends AbstractModule {

    @Slf4j
    public static class RabbitMQContext implements ApiContext {
        private final Sender sender;
        private final SendOptions sendOptions;
        private final AcknowledgableDelivery delivery;
        private final Optional<Tracer> tracerOpt;
        private final String operationName;
        @Getter(lazy = true, value = AccessLevel.PRIVATE)
        private final Map<String, String> headers = ofNullable(delivery.getProperties())
                .map(BasicProperties::getHeaders)
                .map(Map::entrySet)
                .filter(J::nonEmpty)
                .map(it -> {
                    final var builder = ImmutableMap.<String, String>builder();
                    it.forEach(entry -> builder.put(entry.getKey(), entry.getValue().toString()));
                    return builder.build();
                })
                .orElseGet(ImmutableMap::of);
        @Getter(lazy = true, value = AccessLevel.PRIVATE)
        private final byte[] body = delivery.getBody();
        @Getter(lazy = true, value = AccessLevel.PRIVATE)
        private final Optional<Span> spanOpt = ofNullable(tracerOpt)
                .flatMap(it -> Util.spanOpt(it, operationName, headers()))
                .map(span -> {
                    final var dest = Stream.of(exchange(), routingKey()).filter(J::nonBlank).collect(joining(":"));
                    return span.setTag(Tags.MESSAGE_BUS_DESTINATION, dest);
                });

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

        public String exchange() {
            return delivery.getEnvelope().getExchange();
        }

        public String routingKey() {
            return delivery.getEnvelope().getRoutingKey();
        }

        @Override
        public Map<String, String> headers() {
            return getHeaders();
        }

        @Override
        public byte[] body() {
            return getBody();
        }

        @Override
        public Optional<Tracer> tracerOpt() {
            return tracerOpt;
        }

        @Override
        public Optional<Span> spanOpt() {
            return getSpanOpt();
        }
    }

    @Provides
    private SendOptions SendOptions() {
        final var exceptionHandler = new ExceptionHandlers.RetrySendingExceptionHandler(
                Duration.ofHours(1), Duration.ofMinutes(5),
                ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
        );
        return new SendOptions().trackReturned(true).exceptionHandler(exceptionHandler);
    }

    @Provides
    private ConsumeOptions ConsumeOptions() {
        final var exceptionHandler = new ExceptionHandlers.RetryAcknowledgmentExceptionHandler(
                Duration.ofDays(1), Duration.ofMinutes(5),
                ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
        );
        return new ConsumeOptions().qos(10).exceptionHandler(exceptionHandler);
    }

    @Provides
    private ConnectionFactory ConnectionFactory(@Named(CONFIG) JsonObject rootConfig) {
        final var config = rootConfig.getJsonObject("rabbit");
        final var connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setHost(config.getString("host"));
        connectionFactory.setUsername(config.getString("username"));
        connectionFactory.setPassword(config.getString("password"));
        return connectionFactory;
    }

    // todo fixme sender
//    @Singleton
    @Provides
    private Sender Sender(ConnectionFactory connectionFactory, @Named(SERVICE) String group) {
        final var connectionMono = singleConnectionMono(() -> {
            final Address address = new Address(connectionFactory.getHost());
            final Address[] addrs = {address};
            return connectionFactory.newConnection(addrs, group + ":sender");
        });
        final var channelPoolOptions = new ChannelPoolOptions().maxCacheSize(10);
        final var senderOptions = new SenderOptions()
                .connectionFactory(connectionFactory)
                .connectionMono(connectionMono)
                .resourceManagementScheduler(Schedulers.boundedElastic())
                .channelPool(ChannelPoolFactory.createChannelPool(connectionMono, channelPoolOptions));
        return RabbitFlux.createSender(senderOptions);
    }

    @Provides
    private Receiver Receiver(ConnectionFactory connectionFactory, @Named(SERVICE) String group) {
        final var receiverOptions = new ReceiverOptions()
                .connectionFactory(connectionFactory)
                .connectionSupplier(cf -> {
                    final Address address = new Address(connectionFactory.getHost());
                    return cf.newConnection(new Address[]{address}, group + ":receiver");
                });
        return RabbitFlux.createReceiver(receiverOptions);
    }

}
