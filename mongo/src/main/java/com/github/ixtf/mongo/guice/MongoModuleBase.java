package com.github.ixtf.mongo.guice;

import com.github.ixtf.mongo.JmongoRefCodecProvider;
import com.github.ixtf.mongo.MongoEntityLoggable;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public abstract class MongoModuleBase extends AbstractModule {

    protected abstract MongoClientSettings.Builder builder(MongoClientSettings.Builder builder);

    protected abstract String database();

    protected abstract CodecRegistry codecRegistry();

    @Singleton
    @Provides
    private CodecRegistry CodecRegistry() {
        final var jmongoRefCodecProvider = new JmongoRefCodecProvider();
        final var pojoCodecProvider = PojoCodecProvider.builder()
                .register(MongoEntityLoggable.Operator.class)
                .build();
        return fromRegistries(getDefaultCodecRegistry(), codecRegistry(), fromProviders(jmongoRefCodecProvider, pojoCodecProvider));
    }

    @Singleton
    @Provides
    private MongoClient MongoClient(CodecRegistry codecRegistry) {
        final var builder = builder(MongoClientSettings.builder())
                .codecRegistry(codecRegistry);
        return MongoClients.create(builder.build());
    }

    @Singleton
    @Provides
    private MongoDatabase MongoDatabase(MongoClient client) {
        return client.getDatabase(database());
    }
}
