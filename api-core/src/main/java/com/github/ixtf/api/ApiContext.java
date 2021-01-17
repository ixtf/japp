package com.github.ixtf.api;

import com.github.ixtf.J;
import com.sun.security.auth.UserPrincipal;
import io.netty.util.AsciiString;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapAdapter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static com.github.ixtf.J.checkAndGetCommand;
import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

public interface ApiContext {

    Map<String, String> headers();

    byte[] body();

    Optional<Tracer> tracerOpt();

    Optional<Span> spanOpt();

    default String bodyAsString() {
        return ofNullable(body()).map(Buffer::buffer).map(it -> it.toString(UTF_8)).orElse(null);
    }

    default <T> T command(Class<T> clazz) {
        return checkAndGetCommand(clazz, body());
    }

    default String header(String key) {
        return headers().get(key);
    }

    default String header(AsciiString key) {
        return headers().get(key.toString());
    }

    default Optional<Principal> principalOpt() {
        return ofNullable(header(Principal.class.getName()))
                .filter(J::nonBlank)
                .map(UserPrincipal::new);
    }

    default Principal principal() {
        return principalOpt().get();
    }

    default Map injectMap() {
        final Map map = J.newHashMap();
        tracerOpt().ifPresent(tracer -> spanOpt().map(Span::context).ifPresent(it -> tracer.inject(it, TEXT_MAP, new TextMapAdapter(map))));
        principalOpt().map(Principal::getName).ifPresent(it -> map.put(Principal.class.getName(), it));
        return map;
    }

    default DeliveryOptions injectDeliveryOptions() {
        final var deliveryOptions = new DeliveryOptions();
        final Map<String, String> map = injectMap();
        map.forEach((k, v) -> deliveryOptions.addHeader(k, v));
        return deliveryOptions;
    }

}
