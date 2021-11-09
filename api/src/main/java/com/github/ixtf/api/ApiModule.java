package com.github.ixtf.api;

import com.google.inject.Module;
import com.google.inject.*;
import com.google.inject.name.Named;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.handler.CorsHandler;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.inject.name.Names.named;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

public class ApiModule extends AbstractModule {
    public static final Named CONFIG = named("__:ApiModule:CONFIG__");
    private static volatile Injector INJECTOR;
    private final Vertx vertx;
    private final JsonObject config;

    private ApiModule(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    synchronized public static void init(Vertx vertx, JsonObject config, Module... modules) {
        if (INJECTOR == null) {
            INJECTOR = Guice.createInjector(Stream.concat(
                    Stream.of(new ApiModule(vertx, config)),
                    ofNullable(modules).stream().flatMap(Arrays::stream)
            ).toArray(Module[]::new));
        }
    }

    public static <T> T getInstance(Class<T> type) {
        return INJECTOR.getInstance(type);
    }

    public static <T> T getInstance(Class<T> type, Annotation annotation) {
        return INJECTOR.getInstance(Key.get(type, annotation));
    }

    public static void injectMembers(Object o) {
        INJECTOR.injectMembers(o);
    }

    @Override
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);
        bind(JsonObject.class).annotatedWith(CONFIG).toInstance(config);
        bind(Tracer.class).toInstance(Configuration.fromEnv("api").getTracer());
    }

    @Singleton
    @Provides
    private OAuth2Options OAuth2Options() {
        final var config = this.config.getJsonObject("keycloak", new JsonObject());
        final var site = config.getString("site");
        final var clientID = config.getString("clientID", "api");
        return new OAuth2Options().setSite(site).setClientId(clientID);
    }

    @Provides
    private CorsHandler CorsHandler() {
        final var config = this.config.getJsonObject("cors", new JsonObject());
        final var allowedOriginPattern = ofNullable(config.getJsonArray("webOrigins"))
                .map(JsonArray::spliterator)
                .map(spliterator -> StreamSupport.stream(spliterator, true)
                        .distinct()
                        .map(it -> "(" + it + ")")
                        .collect(joining("|"))
                )
                .filter(it -> !it.isBlank())
                .orElse(".*.");
        final var allowedHeaders = Set.of(
                ACCESS_CONTROL_REQUEST_METHOD.toString(),
                ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(),
                ACCESS_CONTROL_ALLOW_ORIGIN.toString(),
                ACCESS_CONTROL_ALLOW_HEADERS.toString(),
                AUTHORIZATION.toString(),
                CACHE_CONTROL.toString(),
                ACCEPT.toString(),
                ORIGIN.toString(),
                CONTENT_TYPE.toString()
        );
        return CorsHandler.create(allowedOriginPattern).allowedHeaders(allowedHeaders).allowCredentials(true);
    }

}
