package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.data.EntityDTO;
import com.google.common.collect.ImmutableList;
import com.mongodb.DBRef;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.security.Principal;
import java.util.Optional;

import static com.github.ixtf.guice.GuiceModule.getInstance;

public class Jmongo {
    public static <T> T find(Class<T> clazz, DBRef dbRef) {
        final var database = Optional.ofNullable(dbRef)
                .map(DBRef::getDatabaseName)
                .filter(J::nonBlank)
                .map(it -> client().getDatabase(it))
                .orElseGet(Jmongo::database);
        final var collection = entityCollection(database, clazz);
        return find(collection, dbRef.getId());
    }

    public static BsonDocument toBsonDocument(Object entity) {
        return toBsonDocument(collection(entity.getClass()), entity);
    }

    public static BsonDocument toBsonDocument(MongoCollection collection, Object entity) {
        final var bsonDocument = new BsonDocument();
        final Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(bsonDocument), entity, EncoderContext.builder().build());
        return bsonDocument;
    }

    public static <T> T find(Class<T> clazz, EntityDTO dto) {
        return find(clazz, dto.getId());
    }

    public static <T> T find(Class<T> clazz, Principal principal) {
        return find(clazz, principal.getName());
    }

    public static <T> T find(Class<T> clazz, Object id) {
        return find(entityCollection(clazz), id);
    }

    public static <T> T find(MongoCollection<T> collection, Object id) {
        final var condition = Filters.eq("_id", id);
        final var iterable = collection.find(condition);
        final var list = ImmutableList.copyOf(iterable);
        if (J.isEmpty(list)) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        throw new RuntimeException("[" + id + "]");
    }

    public static MongoCollection<Document> collection(Class<?> clazz) {
        return collection(database(), clazz);
    }

    public static <T> MongoCollection<T> entityCollection(Class<T> clazz) {
        return entityCollection(database(), clazz);
    }

    public static MongoCollection<Document> collection(MongoDatabase database, Class<?> clazz) {
        return database.getCollection(collectionName(clazz));
    }

    public static <T> MongoCollection<T> entityCollection(MongoDatabase database, Class<T> clazz) {
        return database.getCollection(collectionName(clazz), clazz);
    }

    public static String collectionName(Class<?> clazz) {
        return "T_" + clazz.getSimpleName();
    }

    public static MongoDatabase database() {
        return getInstance(MongoDatabase.class);
    }

    public static MongoClient client() {
        return getInstance(MongoClient.class);
    }
}
