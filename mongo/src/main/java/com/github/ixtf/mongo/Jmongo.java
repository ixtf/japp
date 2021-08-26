package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.data.EntityDTO;
import com.github.ixtf.persistence.IEntity;
import com.github.ixtf.persistence.Sort;
import com.mongodb.client.model.Sorts;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toUnmodifiableList;

public class Jmongo {
    public static final String ID_COL = "_id";
    public static final String DELETED_COL = "deleted";
    public static final Mono<Bson> DELETED_FILTER$ = Mono.fromCallable(() -> eq(DELETED_COL, false));
    public static final int DEFAULT_BATCH_SIZE = 10_000;

    public static Mono<Document> $match(Flux<Bson> condition$) {
        return Flux.merge(condition$, DELETED_FILTER$).collectList().map(it -> new Document("$match", and(it)));
    }

    public static Bson $skip(int skip) {
        return new Document("$skip", skip);
    }

    public static Bson $limit(int limit) {
        return new Document("$limit", limit);
    }

    public static Bson $count() {
        return new Document("$count", "count");
    }

    public static Optional<Bson> $sortOpt(Collection<Sort> sorts) {
        return sortOpt(sorts).map(it -> new Document("$sort", it));
    }

    public static Optional<Bson> sortOpt(Collection<Sort> sorts) {
        final var collect = ofNullable(sorts).filter(J::nonEmpty).stream().flatMap(Collection::stream).map(it -> {
            final var id = it.getId();
            switch (it.getStart()) {
                case asc:
                    return Sorts.ascending(id);
                default:
                    return Sorts.descending(id);
            }
        }).collect(toUnmodifiableList());
        return Optional.of(collect).filter(J::nonEmpty).map(Sorts::orderBy);
    }

    public MongoUnitOfWork uow() {
        return new MongoUnitOfWork(this);
    }

    public <T> Mono<T> find(MongoCollection<T> collection, Object id) {
        return Flux.defer(() -> collection.find(eq(ID_COL, id)).batchSize(1)).next();
    }

    public boolean exists(Class clazz, Object id) {
        return Mono.from(collection(clazz).countDocuments(eq(ID_COL, id))).block() > 0;
    }

    public boolean exists(IEntity entity) {
        return exists(entity.getClass(), entity.getId());
    }

    public <T> FindPublisher<T> findPublisher(MongoCollection<T> collection, Publisher<Bson> filter$) {
        return Flux.defer(() -> filter$).collectList().map(filters -> {
            if (J.isEmpty(filters)) {
                return collection.find().batchSize(DEFAULT_BATCH_SIZE);
            } else if (filters.size() == 1) {
                return collection.find(filters.get(0)).batchSize(DEFAULT_BATCH_SIZE);
            } else {
                return collection.find(and(filters)).batchSize(DEFAULT_BATCH_SIZE);
            }
        }).block();
    }

    public Mono<Long> countCollection(MongoCollection collection, Publisher<Bson> filter$) {
        return Flux.defer(() -> filter$).collectList().flatMapMany(filters -> {
            if (J.isEmpty(filters)) {
                return collection.countDocuments();
            } else if (filters.size() == 1) {
                return collection.countDocuments(filters.get(0));
            } else {
                return collection.countDocuments(and(filters));
            }
        }).next();
    }

    public <T> Mono<T> find(Class<T> clazz, Object id) {
        // todo add cache
        return find(entityCollection(clazz), id);
    }

    public <T> Mono<T> find(Class<T> clazz, Principal principal) {
        return find(clazz, principal.getName());
    }

    public <T> Mono<T> find(Class<T> clazz, EntityDTO dto) {
        return find(clazz, dto.getId());
    }

    public MongoClient client() {
        return getInstance(MongoClient.class);
    }

    public MongoDatabase database() {
        return getInstance(MongoDatabase.class);
    }

    public MongoDatabase database(Class<?> clazz) {
        return ofNullable(clazz.getAnnotation(MongoEntity.class))
                .map(MongoEntity::database)
                .filter(J::nonBlank)
                .map(client()::getDatabase)
                .orElseGet(this::database);
    }

    public String collectionName(Class<?> clazz) {
        return ofNullable(clazz.getAnnotation(MongoEntity.class))
                .map(MongoEntity::collection)
                .filter(J::nonBlank)
                .orElseGet(() -> "T_" + clazz.getSimpleName());
    }

    public MongoCollection<Document> collection(Class<?> clazz) {
        return database(clazz).getCollection(collectionName(clazz));
    }

    public <T> MongoCollection<T> entityCollection(Class<T> clazz) {
        return database(clazz).getCollection(collectionName(clazz), clazz);
    }

    public <T> MongoCollection<BsonDocument> bsonDocumentCollection(Class<T> clazz) {
        return database(clazz).getCollection(collectionName(clazz), BsonDocument.class);
    }

    public BsonDocument toBsonDocument(Object entity) {
        final var bsonDocument = new BsonDocument();
        final var writer = new BsonDocumentWriter(bsonDocument);
        final var encoderContext = EncoderContext.builder().build();
        final var codecRegistry = getInstance(CodecRegistry.class);
        final Codec codec = codecRegistry.get(entity.getClass());
        codec.encode(writer, entity, encoderContext);
        return bsonDocument;
    }

    public <T> T fromBsonDocument(BsonDocument bsonDocument, Class<T> clazz) {
        final var reader = new BsonDocumentReader(bsonDocument);
        final var registry = getInstance(CodecRegistry.class);
        final var codec = registry.get(clazz);
        return codec.decode(reader, DecoderContext.builder().build());
    }

    public <T extends MongoEntityBase> Flux<T> list(Class<T> clazz) {
        return Flux.defer(() -> findPublisher(entityCollection(clazz), DELETED_FILTER$));
    }

    public <T> Flux<T> query(Class<T> clazz, Publisher<Bson> filter$) {
        return Flux.defer(() -> findPublisher(entityCollection(clazz), Flux.merge(filter$, DELETED_FILTER$)));
    }

    public <T> Flux<T> query(Class<T> clazz, Stream<Bson> stream) {
        return query(clazz, Mono.justOrEmpty(stream).flatMapMany(Flux::fromStream));
    }

    public <T> Flux<T> query(Class<T> clazz, Iterable<Bson> iterable) {
        return query(clazz, Mono.justOrEmpty(iterable).flatMapMany(Flux::fromIterable));
    }

    public <T> Flux<T> query(Class<T> clazz, Publisher<Bson> filter$, int skip, int limit) {
        return Flux.defer(() -> findPublisher(entityCollection(clazz), Flux.merge(filter$, DELETED_FILTER$)).skip(skip).limit(limit).batchSize(limit));
    }

    public <T> Flux<T> query(Class<T> clazz, Stream<Bson> stream, int skip, int limit) {
        return query(clazz, Flux.fromStream(stream), skip, limit);
    }

    public <T> Flux<T> query(Class<T> clazz, Iterable<Bson> iterable, int skip, int limit) {
        return query(clazz, Flux.fromIterable(iterable), skip, limit);
    }

    public Mono<Long> count(MongoCollection collection, Publisher<Bson> filter$) {
        return countCollection(collection, Flux.merge(filter$, DELETED_FILTER$));
    }

    public Mono<Long> count(Class clazz, Publisher<Bson> filter$) {
        return count(collection(clazz), filter$);
    }

    public Mono<Long> count(Class clazz, Stream<Bson> stream) {
        return count(clazz, Mono.justOrEmpty(stream).flatMapMany(Flux::fromStream));
    }

    public Mono<Long> count(Class clazz, Iterable<Bson> iterable) {
        return count(clazz, Mono.justOrEmpty(iterable).flatMapMany(Flux::fromIterable));
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Publisher<Bson> filter$, int limit) {
        final var max = Math.max(limit, 10);
        return Flux.defer(() -> findPublisher(entityCollection(clazz), Flux.merge(filter$, DELETED_FILTER$)).limit(max).batchSize(max));
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Stream<Bson> stream, int limit) {
        return autocomplete(clazz, Mono.justOrEmpty(stream).flatMapMany(Flux::fromStream), limit);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Iterable<Bson> iterable, int limit) {
        return autocomplete(clazz, Mono.justOrEmpty(iterable).flatMapMany(Flux::fromIterable), limit);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Publisher<Bson> filter$) {
        return autocomplete(clazz, filter$, 10);
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Stream<Bson> stream) {
        return autocomplete(clazz, Flux.fromStream(stream));
    }

    public <T> Flux<T> autocomplete(Class<T> clazz, Iterable<Bson> iterable) {
        return autocomplete(clazz, Flux.fromIterable(iterable));
    }

}
