package com.github.ixtf.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ixtf.J;
import com.sun.security.auth.UserPrincipal;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapAdapter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;
import static java.util.Optional.ofNullable;

public class Util {

    public static <T> T checkAndGetCommand(Class<T> clazz, JsonObject jsonObject) {
        return J.checkAndGetCommand(clazz, jsonObject.encode());
    }

    public static <T> T checkAndGetCommand(Class<T> clazz, JsonArray jsonArray) {
        return J.checkAndGetCommand(clazz, jsonArray.encode());
    }

    public static JsonObject config(final String env, final String defaultV) {
        return ofNullable(System.getenv(env))
                .filter(J::nonBlank)
                .or(() -> ofNullable(defaultV))
                .map(J::getFile)
                .filter(File::exists)
                .filter(File::canRead)
                .map(J::readYaml)
                .map(JsonNode::toString)
                .map(JsonObject::new)
                .orElseGet(JsonObject::new);
    }

    public static Optional<Principal> principalOpt(final Map map) {
        return ofNullable(map)
                .map(it -> it.get(Principal.class.getName()))
                .map(Object::toString)
                .filter(J::nonBlank)
                .map(UserPrincipal::new);
    }

    public static Optional<Span> spanOpt(final Optional<Tracer> tracerOpt, final String operationName, final Map map) {
        return tracerOpt.map(tracer -> {
            final var spanBuilder = tracer.buildSpan(operationName);
            ofNullable(tracer.extract(TEXT_MAP, new TextMapAdapter(map))).ifPresent(spanBuilder::asChildOf);
            return spanBuilder.start();
        });
    }

}
