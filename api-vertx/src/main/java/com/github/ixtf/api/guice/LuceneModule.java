package com.github.ixtf.api.guice;

import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.idl.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.UploadScalar;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.github.ixtf.api.guice.ApiModule.CONFIG;
import static java.nio.charset.StandardCharsets.UTF_8;

public class LuceneModule extends AbstractModule {
    public static final String DIR = "com.github.ixtf.persistence.mongo.guice.LuceneModule:__DIR__";

    @Named(DIR)
    @Singleton
    @Provides
    private String Lucene_DIR(@Named(CONFIG) JsonObject rootConfig) {
        final var config = rootConfig.getJsonObject("lucene");
        return config.getString("dir");
    }

    protected RuntimeWiring buildRuntimeWiring(final Map<String, Map<String, Method>> map) {
        final var runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
                .scalar(ExtendedScalars.Object)
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(UploadScalar.build());
        runtimeWiringBuilder.wiringFactory(new WiringFactory() {
            @Override
            public DataFetcher getDataFetcher(FieldWiringEnvironment env) {
                final var fieldDefinition = env.getFieldDefinition();
                final var type = fieldDefinition.getType();
                final var name = fieldDefinition.getName();
                final var method = map.get(type).get(name);
                return environment -> null;
            }
        });
//        map.forEach((typeName, methodMap) -> runtimeWiringBuilder.type(typeName, builder -> {
//            methodMap.forEach((fieldName, method) -> builder.dataFetcher(fieldName, new ReplyDataFetcher(method)));
//            return builder;
//        }));
        return runtimeWiringBuilder.build();
    }

    protected TypeDefinitionRegistry TypeDefinitionRegistry() throws IOException {
        final var schemaParser = new SchemaParser();
        final var resource = Resources.getResource("schema.graphql");
        final var charSource = Resources.asCharSource(resource, UTF_8);
        return schemaParser.parse(charSource.read());
    }

}
