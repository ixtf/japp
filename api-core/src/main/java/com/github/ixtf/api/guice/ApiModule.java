package com.github.ixtf.api.guice;

import com.github.ixtf.api.ApiAction;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.github.classgraph.ClassGraph;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableSet;

public abstract class ApiModule extends AbstractModule {
    public static final String SERVICE = "__:ApiModule:SERVICE:__";
    public static final String CONFIG = "__:ApiModule:CONFIG:__";
    public static final String ACTIONS = "__:ApiModule:ACTIONS:__";
    private final Vertx vertx;
    private final String service;
    private final JsonObject config;

    protected ApiModule(Vertx vertx, String service, JsonObject config) {
        this.vertx = vertx;
        this.service = service;
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);
        bind(String.class).annotatedWith(Names.named(SERVICE)).toInstance(service);
        bind(JsonObject.class).annotatedWith(Names.named(CONFIG)).toInstance(config);
        OptionalBinder.newOptionalBinder(binder(), Tracer.class);
    }

    @Named(ACTIONS)
    @Singleton
    @Provides
    private Collection<Method> ACTIONS() {
        return new ClassGraph()
                .enableAllInfo()
                .acceptPackages(ActionPackages().toArray(String[]::new))
                .acceptClasses(ActionClasses().toArray(String[]::new))
                .scan()
                .getClassesWithMethodAnnotation(ApiAction.class.getName())
                .loadClasses()
                .parallelStream()
                .map(Class::getMethods)
                .flatMap(Arrays::stream)
                .parallel()
                .filter(it -> Objects.nonNull(it.getAnnotation(ApiAction.class)))
                .collect(toUnmodifiableSet());
    }

    protected Collection<String> ActionPackages() {
        return List.of(this.getClass().getPackageName());
    }

    protected Collection<String> ActionClasses() {
        return List.of();
    }

    @Singleton
    @Provides
    private Tracer Tracer() {
        return ofNullable(config.getJsonObject("tracer")).map(it -> {
            final var serviceName = it.getString("serviceName", service);
            final var agentHost = it.getString("agentHost", "dev.medipath.com.cn");
            final var samplerConfig = Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1);
            final var senderConfiguration = new Configuration.SenderConfiguration().withAgentHost(agentHost);
            final var reporterConfig = Configuration.ReporterConfiguration.fromEnv().withSender(senderConfiguration).withLogSpans(true);
            return new Configuration(serviceName).withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
        }).orElse(null);
    }
}
