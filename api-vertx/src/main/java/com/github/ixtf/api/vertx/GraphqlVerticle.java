package com.github.ixtf.api.vertx;

import com.github.ixtf.api.GraphqlAction;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.UploadScalar;
import io.vertx.ext.web.handler.graphql.impl.GraphQLInput;
import io.vertx.ext.web.handler.graphql.impl.GraphQLQuery;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static com.github.ixtf.api.guice.ApiModule.GRAPHQL_ACTION_MAP;
import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static graphql.ExecutionInput.newExecutionInput;
import static java.nio.charset.StandardCharsets.UTF_8;

public class GraphqlVerticle extends AbstractVerticle implements Handler<Message<Buffer>> {
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private static final GraphQL graphQL = _graphQL();
    @Inject
    private Optional<Tracer> tracerOpt;

    protected static String address() {
        return "";
    }

    protected static GraphQL _graphQL() {
        final var typeDefinitionRegistry = buildTypeDefinitionRegistry();
        final var runtimeWiring = buildRuntimeWiring(getInstance(Map.class, Names.named(GRAPHQL_ACTION_MAP)));
        final var schemaGenerator = new SchemaGenerator();
        final var graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    protected static RuntimeWiring buildRuntimeWiring(final Map<String, Map<String, Method>> map) {
        final var runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.Object)
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(UploadScalar.build());
        map.forEach((typeName, methodMap) -> methodMap.forEach((fieldName, method) -> runtimeWiringBuilder.type(typeName, builder -> {
            final var dataFetcher = new ReplyDataFetcher(method);
            return builder.dataFetcher(fieldName, dataFetcher);
        })));
        return runtimeWiringBuilder.build();
    }

    @SneakyThrows(IOException.class)
    protected static TypeDefinitionRegistry buildTypeDefinitionRegistry() {
        final var schemaParser = new SchemaParser();
        final var resource = Resources.getResource("schema.graphql");
        final var charSource = Resources.asCharSource(resource, UTF_8);
        return schemaParser.parse(charSource.read());
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);
        vertx.eventBus().consumer(address(), this).completionHandler(startPromise);
    }

    @Override
    public void handle(Message<Buffer> reply) {
        final var ctx = new VertxContext(reply, tracerOpt);
        final var spanOpt = ctx.spanOpt();
        Mono.fromCompletionStage(() -> {
            final var graphQLInput = GraphQLInput.decode(reply.body());
            // todo GraphQLBatch
            final var graphQLQuery = (GraphQLQuery) graphQLInput;
            final var executionInput = newExecutionInput()
                    .query(graphQLQuery.getQuery())
                    .build();
            return graphQL.executeAsync(executionInput);
        }).map(executionResult -> {
            final var specification = executionResult.toSpecification();
            final var jsonObject = new JsonObject(specification);
            return jsonObject.toBuffer();
        }).subscribe(it -> {
            reply.reply(it);
            spanOpt.ifPresent(Span::finish);
        }, e -> {
            reply.fail(400, e.getMessage());
            spanOpt.ifPresent(span -> span.setTag(Tags.ERROR, true).log(e.getMessage()).finish());
        });
    }

    private static class ReplyDataFetcher implements DataFetcher {
        private final Object instance;
        private final Method method;
        private final String address;
        @Getter
        private final String operationName;
        private final Logger log;

        private ReplyDataFetcher(Method method) {
            this.method = method;

            final var declaringClass = method.getDeclaringClass();
            instance = getInstance(declaringClass);

            final var annotation = method.getAnnotation(GraphqlAction.class);
            final var type = annotation.type();
            final var action = annotation.action();
            address = String.join(":", address(), type, action);

            operationName = String.join(":", instance.getClass().getName(), method.getName());
            log = LoggerFactory.getLogger(instance.getClass());
        }

        @Override
        public Object get(DataFetchingEnvironment env) throws Exception {
            return null;
        }
    }

}
