package com.github.ixtf.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ixtf.J;
import com.google.common.io.Resources;
import com.sun.security.auth.UserPrincipal;
import graphql.schema.DataFetchingEnvironment;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapAdapter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import static com.github.ixtf.Constant.MAPPER;
import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;

public class Util {

    @SneakyThrows(IOException.class)
    public static String loadGraphql(String s) {
        final var url = Resources.getResource(s);
        return Resources.toString(url, UTF_8);
    }

    public static <T> T checkAndGetCommand(DataFetchingEnvironment env, Class<T> clazz) {
        final T command = MAPPER.convertValue(env.getArgument("command"), clazz);
        return J.checkAndGetCommand(command);
    }

    public static Principal principal(DataFetchingEnvironment env) {
        final var ctx = env.getGraphQlContext();
        return new UserPrincipal(ctx.get(Principal.class.getName()));
    }

    public static <T> T checkAndGetCommand(Class<T> clazz, JsonObject jsonObject) {
        return J.checkAndGetCommand(clazz, jsonObject.encode());
    }

    public static <T> T checkAndGetCommand(Class<T> clazz, JsonArray jsonArray) {
        return J.checkAndGetCommand(clazz, jsonArray.encode());
    }

    public static JsonObject config(final String env, final String defaultV) {
        return ofNullable(System.getProperty(env))
                .filter(J::nonBlank)
                .or(() -> ofNullable(System.getenv(env)))
                .filter(J::nonBlank)
                .or(() -> ofNullable(defaultV))
                .map(J::getFile)
                .filter(File::exists)
                .filter(File::canRead)
                .map(J::readJson)
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
