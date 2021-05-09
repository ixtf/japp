package com.github.ixtf.api.guice;

import com.github.ixtf.api.ApiAction;
import com.github.ixtf.api.GraphqlAction;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import graphql.GraphQL;
import graphql.schema.idl.SchemaGenerator;
import io.github.classgraph.ClassGraph;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;

public abstract class ApiModule extends AbstractModule {
    public static final String SERVICE = "com.github.ixtf.api.guice:__SERVICE__";
    public static final String CONFIG = "com.github.ixtf.api.guice:__CONFIG__";
    public static final String ACTIONS = "com.github.ixtf.api.guice:__ACTIONS__";
    public static final String GRAPHQL_ACTION_MAP = "com.github.ixtf.api.guice:__GRAPHQL_ACTION_MAP__";

    protected final Vertx vertx;
    protected final String service;
    protected final JsonObject config;

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

    protected void bindConfig(String annotatedWith, String key) {
        bind(JsonObject.class).annotatedWith(Names.named(annotatedWith)).toInstance(config.getJsonObject(key, new JsonObject()));
    }

    protected Stream<Method> streamMethod(Class annotationClass) {
        return new ClassGraph()
                .enableAllInfo()
                .acceptPackages(ActionPackages().toArray(String[]::new))
                .acceptClasses(ActionClasses().toArray(String[]::new))
                .scan()
                .getClassesWithMethodAnnotation(annotationClass.getName())
                .loadClasses()
                .parallelStream()
                .map(Class::getMethods)
                .flatMap(Arrays::stream)
                .parallel()
                .filter(it -> Objects.nonNull(it.getAnnotation(annotationClass)));
    }

    @Named(ACTIONS)
    @Singleton
    @Provides
    private Collection<Method> ACTIONS() {
        final var ret = streamMethod(ApiAction.class).collect(toUnmodifiableSet());
        ret.parallelStream().collect(groupingBy(it -> {
            final var annotation = it.getAnnotation(ApiAction.class);
            final var service = annotation.service();
            final var action = annotation.action();
            return String.join(":", service, action);
        })).forEach((k, v) -> {
            if (v.size() > 1) {
                throw new RuntimeException("api地址重复 [" + k + "]");
            }
        });
        return ret;
    }

    @Named(GRAPHQL_ACTION_MAP)
    @Singleton
    @Provides
    private Map<String, Map<String, Method>> GRAPHQL_ACTION_MAP() {
        return streamMethod(GraphqlAction.class).collect(toUnmodifiableMap(it -> {
            final var annotation = it.getAnnotation(GraphqlAction.class);
            return annotation.type();
        }, it -> {
            final var annotation = it.getAnnotation(GraphqlAction.class);
            return Map.of(annotation.action(), it);
        }, (a, b) -> ImmutableMap.<String, Method>builder().putAll(a).putAll(b).build()));
    }

    @Named(GRAPHQL_ACTION_MAP)
    @Singleton
    @Provides
    private GraphQL GraphQL(@Named(GRAPHQL_ACTION_MAP) Map<String, Map<String, Method>> MAP) throws IOException {
//        final var typeDefinitionRegistry = TypeDefinitionRegistry();
//        final var runtimeWiring = buildRuntimeWiring(GRAPHQL_ACTION_MAP);
        final var schemaGenerator = new SchemaGenerator();
//        final var graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
//        return GraphQL.newGraphQL(graphQLSchema).build();
        return null;
    }

    protected abstract Collection<String> ActionPackages();

    protected Collection<String> ActionClasses() {
        return List.of();
    }

    @Singleton
    @Provides
    private Tracer Tracer() {
        return ofNullable(config.getJsonObject("tracer")).map(it -> {
            final var serviceName = it.getString("serviceName", service);
            final var agentHost = it.getString("agentHost");
            final var samplerConfig = Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1);
            final var senderConfiguration = new Configuration.SenderConfiguration().withAgentHost(agentHost);
            final var reporterConfig = Configuration.ReporterConfiguration.fromEnv().withSender(senderConfiguration).withLogSpans(true);
            return new Configuration(serviceName).withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
        }).orElse(null);
    }
}
