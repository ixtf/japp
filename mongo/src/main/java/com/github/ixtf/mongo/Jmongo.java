package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.data.EntityDTO;
import com.github.ixtf.persistence.IEntity;
import com.google.common.collect.ImmutableList;
import com.mongodb.DBRef;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;

import java.security.Principal;
import java.util.Optional;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.mongodb.client.model.Filters.eq;

public class Jmongo {
    public static final String ID_COL = "_id";
    public static final String DELETED_COL = "deleted";

    public MongoUnitOfWork uow() {
        return new MongoUnitOfWork(this);
    }

    public <T> T find(Class<T> clazz, Principal principal) {
        return find(clazz, principal.getName());
    }

    public <T> T find(Class<T> clazz, EntityDTO dto) {
        return find(clazz, dto.getId());
    }

    public <T> T find(Class<T> clazz, DBRef dbRef) {
        final var database = Optional.ofNullable(dbRef)
                .map(DBRef::getDatabaseName)
                .filter(J::nonBlank)
                .map(it -> client().getDatabase(it))
                .orElseGet(this::database);
        final var collection = database.getCollection(dbRef.getCollectionName(), clazz);
        return find(collection, dbRef.getId());
    }

    public MongoClient client() {
        return getInstance(MongoClient.class);
    }

    public MongoDatabase database() {
        return getInstance(MongoDatabase.class);
    }

    public MongoDatabase database(Class<?> clazz) {
        return Optional.ofNullable(clazz.getAnnotation(MongoEntity.class))
                .map(MongoEntity::database)
                .filter(J::nonBlank)
                .map(it -> client().getDatabase(it))
                .orElseGet(this::database);
    }

    public MongoCollection<Document> collection(Class<?> clazz) {
        final var database = database(clazz);
        return Optional.ofNullable(clazz.getAnnotation(MongoEntity.class))
                .map(MongoEntity::collection)
                .filter(J::nonBlank)
                .map(it -> database.getCollection(it))
                .orElseGet(() -> database.getCollection("T_" + clazz.getSimpleName()));
    }

    public <T> MongoCollection<T> entityCollection(Class<T> clazz) {
        final var database = database(clazz);
        return Optional.ofNullable(clazz.getAnnotation(MongoEntity.class))
                .map(MongoEntity::collection)
                .filter(J::nonBlank)
                .map(it -> database.getCollection(it, clazz))
                .orElseGet(() -> database.getCollection("T_" + clazz.getSimpleName(), clazz));
    }

    public BsonDocument toBsonDocument(Object entity) {
        final var clazz = entity.getClass();
        final var collection = entityCollection(clazz);
        final var codecRegistry = collection.getCodecRegistry();
        final Codec codec = codecRegistry.get(clazz);
        final var bsonDocument = new BsonDocument();
        final var writer = new BsonDocumentWriter(bsonDocument);
        final var encoderContext = EncoderContext.builder().build();
        codec.encode(writer, entity, encoderContext);
        return bsonDocument;
    }

    public <T> T find(Class<T> clazz, Object id) {
        // todo add cache
        return find(entityCollection(clazz), id);
    }

    public <T> T find(MongoCollection<T> collection, Object id) {
        final var condition = eq(ID_COL, id);
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

    public boolean exists(IEntity entity) {
        final var collection = collection(entity.getClass());
        final var condition = eq(ID_COL, entity.getId());
        return collection.countDocuments(condition) > 0;
    }
}
