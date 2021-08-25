package com.github.ixtf.api.guice;

import com.github.ixtf.J;
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
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.idl.*;
import io.github.classgraph.ClassGraph;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import io.vertx.ext.web.handler.graphql.schema.VertxPropertyDataFetcher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableSet;

public abstract class ApiModule extends AbstractModule {
    public static final String SERVICE = "com.github.ixtf.api.guice:__SERVICE__";
    public static final String CONFIG = "com.github.ixtf.api.guice:__CONFIG__";
    public static final String ACTIONS = "com.github.ixtf.api.guice:__ACTIONS__";
    public static final String GRAPHQL_ACTIONS = "com.github.ixtf.api.guice:__GRAPHQL_ACTIONS__";
    public static final String GRAPHQL_ADDRESS = "com.github.ixtf.api.guice:__GRAPHQL_ADDRESS__";

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
        bind(String.class).annotatedWith(Names.named(GRAPHQL_ADDRESS)).toInstance(service + ":graphql");
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

    protected Stream<Class<?>> streamClass(Class annotationClass) {
        return new ClassGraph()
                .enableAllInfo()
                .acceptPackages(ActionPackages().toArray(String[]::new))
                .acceptClasses(ActionClasses().toArray(String[]::new))
                .scan()
                .getClassesWithAnnotation(annotationClass.getName())
                .loadClasses()
                .parallelStream();
    }

    @Named(ACTIONS)
    @Singleton
    @Provides
    private Collection<Method> ACTIONS() {
        final var ret = streamMethod(ApiAction.class).collect(toUnmodifiableSet());
        ret.parallelStream().collect(groupingBy(it -> {
            final var annotation = it.getAnnotation(ApiAction.class);
            final var service = ofNullable(annotation.service()).filter(J::nonBlank).orElse(this.service);
            final var action = annotation.action();
            return String.join(":", service, action);
        })).forEach((k, v) -> {
            if (v.size() > 1) {
                throw new RuntimeException("api地址重复 [" + k + "]");
            }
        });
        return ret;
    }

    @Singleton
    @Provides
    private GraphQL GraphQL(TypeDefinitionRegistry typeDefinitionRegistry, @Named(GRAPHQL_ACTIONS) Collection<Class<?>> classes) {
        final var runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLShort)
                .scalar(ExtendedScalars.GraphQLByte)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.GraphQLBigInteger)
                .scalar(ExtendedScalars.GraphQLChar)
                .scalar(ExtendedScalars.PositiveInt)
                .scalar(ExtendedScalars.NegativeInt)
                .scalar(ExtendedScalars.NonPositiveInt)
                .scalar(ExtendedScalars.NonNegativeInt)
                .scalar(ExtendedScalars.PositiveFloat)
                .scalar(ExtendedScalars.NegativeFloat)
                .scalar(ExtendedScalars.NonPositiveFloat)
                .scalar(ExtendedScalars.NonNegativeFloat)
                .scalar(ExtendedScalars.Url)
                .scalar(ExtendedScalars.Locale)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Time)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Object)
                .scalar(ExtendedScalars.Json)
                .wiringFactory(new WiringFactory() {
                    @Override
                    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
                        return VertxPropertyDataFetcher.create(environment.getFieldDefinition().getName());
                    }
                });
        prepareRuntimeWiring(runtimeWiringBuilder);
        final var queryBuilder = ImmutableMap.<String, DataFetcher>builder();
        final var mutationBuilder = ImmutableMap.<String, DataFetcher>builder();
        classes.forEach(clazz -> {
            final var annotation = clazz.getAnnotation(GraphqlAction.class);
            final var action = annotation.action();
            final var instance = (BiConsumer) getInstance(clazz);
            final var dataFetcher = VertxDataFetcher.create(instance);
            switch (annotation.type()) {
                case QUERY: {
                    queryBuilder.put(action, dataFetcher);
                }
                case MUTATION: {
                    mutationBuilder.put(action, dataFetcher);
                }
            }
        });
        final var runtimeWiring = runtimeWiringBuilder
                .type("Query", builder -> builder.dataFetchers(queryBuilder.build()))
                .type("Mutation", builder -> builder.dataFetchers(mutationBuilder.build()))
                .build();
        final var schemaGenerator = new SchemaGenerator();
        final var graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    protected void prepareRuntimeWiring(RuntimeWiring.Builder builder) {
    }

    @Named(GRAPHQL_ACTIONS)
    @Singleton
    @Provides
    private Collection<Class<?>> GRAPHQL_ACTIONS() {
        final var ret = streamClass(GraphqlAction.class).collect(toUnmodifiableSet());
        ret.parallelStream().collect(groupingBy(it -> {
            final var annotation = it.getAnnotation(GraphqlAction.class);
            final var type = annotation.type();
            final var action = annotation.action();
            return String.join(":", type.name(), action);
        })).forEach((k, v) -> {
            if (v.size() > 1) {
                throw new RuntimeException("graphql地址重复 [" + k + "]");
            }
        });
        return ret;
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
