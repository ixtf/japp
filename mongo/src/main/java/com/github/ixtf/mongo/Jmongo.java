package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.data.EntityDTO;
import com.github.ixtf.persistence.IEntity;
import com.mongodb.DBRef;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class Jmongo {
    public static final String ID_COL = "_id";
    public static final String DELETED_COL = "deleted";
    public static final Bson DELETED_FILTER = eq(DELETED_COL, false);

    public MongoUnitOfWork uow() {
        return new MongoUnitOfWork(this);
    }

    public <T> Mono<T> find(Class<T> clazz, Object id) {
        // todo add cache
        return find(entityCollection(clazz), id);
    }

    public Mono<Long> count(Class clazz, Stream<Bson> filterStream) {
        final var filter = Stream.concat(filterStream, Stream.of(DELETED_FILTER)).toArray(Bson[]::new);
        return Mono.from(countPublisher(clazz, filter));
    }

    public <T> Flux<T> query(Class<T> clazz, Stream<Bson> filterStream) {
        final var filter = Stream.concat(filterStream, Stream.of(DELETED_FILTER)).toArray(Bson[]::new);
        return Flux.defer(() -> findPublisher(clazz, filter));
    }

    public <T> Flux<T> query(Class<T> clazz, Stream<Bson> filterStream, int skip, int limit) {
        final var filter = Stream.concat(filterStream, Stream.of(DELETED_FILTER)).toArray(Bson[]::new);
        return Flux.defer(() -> findPublisher(clazz, filter).batchSize(limit).skip(skip).limit(limit));
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

    public <T> Mono<T> find(MongoCollection<T> collection, Object id) {
        return Flux.defer(() -> {
            final var condition = eq(ID_COL, id);
            return collection.find(condition);
        }).collectList().flatMap(list -> {
            if (J.isEmpty(list)) {
                return Mono.empty();
            }
            if (list.size() == 1) {
                return Mono.just(list.get(0));
            }
            return Mono.error(new RuntimeException("[" + id + "]"));
        });
    }

    public <T> Mono<T> find(Class<T> clazz, DBRef dbRef) {
        final var collection = Optional.ofNullable(dbRef)
                .map(DBRef::getDatabaseName)
                .filter(J::nonBlank)
                .map(it -> client().getDatabase(it))
                .orElseGet(this::database)
                .getCollection(dbRef.getCollectionName(), clazz);
        return find(collection, dbRef.getId());
    }

    public <T> Mono<T> find(Class<T> clazz, Principal principal) {
        return find(clazz, principal.getName());
    }

    public <T> Mono<T> find(Class<T> clazz, EntityDTO dto) {
        return find(clazz, dto.getId());
    }

    public Mono<Long> count(Class clazz, Bson... filters) {
        return count(clazz, Arrays.stream(filters));
    }

    public Mono<Long> count(Class clazz, Optional<Bson>... filters) {
        return count(clazz, Arrays.stream(filters).flatMap(Optional::stream));
    }

    public Mono<Long> count(Class clazz, Iterable<Bson> filters) {
        return count(clazz, Flux.fromIterable(filters).toStream());
    }

    public <T> Flux<T> query(Class<T> clazz, Bson... filters) {
        return query(clazz, Arrays.stream(filters));
    }

    public <T> Flux<T> query(Class<T> clazz, Optional<Bson>... filters) {
        return query(clazz, Arrays.stream(filters).flatMap(Optional::stream));
    }

    public <T> Flux<T> query(Class<T> clazz, Iterable<Bson> filters) {
        return query(clazz, Flux.fromIterable(filters).toStream());
    }

    public <T> Flux<T> query(Class<T> clazz, int skip, int limit, Bson... filters) {
        return query(clazz, Arrays.stream(filters), skip, limit);
    }

    public <T> Flux<T> query(Class<T> clazz, int skip, int limit, Optional<Bson>... filters) {
        return query(clazz, Arrays.stream(filters).flatMap(Optional::stream), skip, limit);
    }

    public <T> Flux<T> query(Class<T> clazz, Iterable<Bson> filters, int skip, int limit) {
        return query(clazz, Flux.fromIterable(filters).toStream(), skip, limit);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Bson... filters) {
        return autocomplete(clazz, 10, filters);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Optional<Bson>... filters) {
        return autocomplete(clazz, 10, filters);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Iterable<Bson> filters) {
        return autocomplete(clazz, filters, 10);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, int limit, Bson... filters) {
        return autocomplete(clazz, Arrays.stream(filters), limit);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, int limit, Optional<Bson>... filters) {
        return autocomplete(clazz, Arrays.stream(filters).flatMap(Optional::stream), limit);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Iterable<Bson> filters, int limit) {
        return autocomplete(clazz, Flux.fromIterable(filters).toStream(), limit);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Stream<Bson> filterStream, int limit) {
        return query(clazz, filterStream, 0, Math.max(limit, 10));
    }

    public <T> FindPublisher<T> findPublisher(Class<T> clazz, Bson... filters) {
        final var collection = entityCollection(clazz);
        return J.isEmpty(filters) ? collection.find() : collection.find(and(filters));
    }

    public Publisher<Long> countPublisher(Class clazz, Bson... filters) {
        final var collection = entityCollection(clazz);
        return J.isEmpty(filters) ? collection.countDocuments() : collection.countDocuments(and(filters));
    }

    public boolean exists(IEntity entity) {
        return exists(entity.getClass(), entity.getId());
    }

    public boolean exists(Class clazz, Object id) {
        return Mono.from(countPublisher(clazz, eq(ID_COL, id))).block() > 0;
    }
}
