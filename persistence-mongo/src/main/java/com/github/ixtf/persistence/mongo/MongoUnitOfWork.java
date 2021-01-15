package com.github.ixtf.persistence.mongo;

import com.github.ixtf.J;
import com.github.ixtf.persistence.IEntity;
import com.github.ixtf.persistence.api.AbstractUnitOfWork;
import com.github.ixtf.persistence.reflection.ClassRepresentation;
import com.github.ixtf.persistence.reflection.ClassRepresentations;
import com.github.ixtf.persistence.reflection.FieldRepresentation;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.reactivestreams.client.MongoCollection;
import jakarta.persistence.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ixtf.persistence.mongo.Jmongo.ID_COL;
import static java.util.stream.Collectors.*;

/**
 * @author jzb 2019-02-18
 */
@Slf4j
public class MongoUnitOfWork extends AbstractUnitOfWork {
    private final Jmongo jmongo;
    private Mono<List<BulkWriteResult>> commitResult;

    MongoUnitOfWork(Jmongo jmongo) {
        this.jmongo = jmongo;
    }

    @Override
    protected boolean exists(IEntity o) {
        return jmongo.exists(o).block();
    }

    @Override
    synchronized public MongoUnitOfWork commit() {
        rxCommit().block();
        return this;
    }

    synchronized public Mono<Void> rxCommit() {
        if (commitResult == null) {
            commitResult = Flux.concat(newListModel(), dirtyListModel(), deleteListModel())
                    .collect(groupingBy(Pair::getKey, LinkedHashMap::new, mapping(Pair::getRight, toList())))
                    .flatMapIterable(Map::entrySet)
                    .flatMap(entry -> {
                        final MongoCollection<Document> collection = jmongo.collection(entry.getKey());
                        return collection.bulkWrite(entry.getValue());
                    })
                    .collectList()
                    .doOnError(err -> log.error("", err))
                    .doOnSuccess(bulkWriteResults -> {
                        if (log.isDebugEnabled()) {
                            log(bulkWriteResults);
                        }
                        newList.stream().forEach(entity ->
                                callbackStream(entity, PostPersist.class).forEach(it -> it.callback(entity))
                        );
                        dirtyList.stream().forEach(entity ->
                                callbackStream(entity, PostUpdate.class).forEach(it -> it.callback(entity))
                        );
                        deleteList.stream().forEach(entity ->
                                callbackStream(entity, PostRemove.class).forEach(it -> it.callback(entity))
                        );
                    })
                    .cache();
        }
        return commitResult.then();
    }

    private Flux<Pair<String, WriteModel<Document>>> newListModel() {
        return Flux.fromIterable(newList).map(o -> {
            callbackStream(o, PrePersist.class).forEach(it -> it.callback(o));
            if (J.isBlank(o.getId())) {
                o.setId(new ObjectId().toHexString());
            }
            final Document document = jmongo.toDocument(o);
            return Pair.of(collectionName(o), new InsertOneModel<>(document));
        });
    }

    private Flux<Pair<String, WriteModel<Document>>> dirtyListModel() {
        return Flux.fromIterable(dirtyList).map(o -> {
            callbackStream(o, PreUpdate.class).forEach(it -> it.callback(o));
            final Document document = jmongo.toDocument(o);
            document.remove(ID_COL);
            final Document $set = new Document("$set", document);
            final UpdateOneModel<Document> model = new UpdateOneModel<>(new Document(ID_COL, idValue(o)), $set);
            return Pair.of(collectionName(o), model);
        });
    }

    private Flux<Pair<String, WriteModel<Document>>> deleteListModel() {
        return Flux.fromIterable(deleteList).map(o -> {
            callbackStream(o, PreRemove.class).forEach(it -> it.callback(o));
            final DeleteOneModel<Document> model = new DeleteOneModel<>(new Document(ID_COL, idValue(o)));
            return Pair.of(collectionName(o), model);
        });
    }

    private String collectionName(Object o) {
        final ClassRepresentation classRepresentation = ClassRepresentations.create(o.getClass());
        return classRepresentation.getTableName();
    }

    @SneakyThrows
    private Object idValue(Object o) {
        final ClassRepresentation classRepresentation = ClassRepresentations.create(o.getClass());
        final Optional<FieldRepresentation> id = classRepresentation.getId();
        final String nativeFieldName = id.map(FieldRepresentation::getFieldName).get();
        return PropertyUtils.getProperty(o, nativeFieldName);
    }

    @Override
    synchronized public MongoUnitOfWork rollback() {
        // todo mongo 需要集群才支持事务，后续实现
        return this;
    }

    private void log(Iterable<BulkWriteResult> bulkWriteResults) {
        final AtomicInteger newCount = new AtomicInteger();
        final AtomicInteger dirtyCount = new AtomicInteger();
        final AtomicInteger deleteCount = new AtomicInteger();
        bulkWriteResults.forEach(bulkWriteResult -> {
            final int insertedCount = bulkWriteResult.getInsertedCount();
            newCount.addAndGet(insertedCount);
            final int modifiedCount = bulkWriteResult.getModifiedCount();
            dirtyCount.addAndGet(modifiedCount);
            final int deletedCount = bulkWriteResult.getDeletedCount();
            deleteCount.addAndGet(deletedCount);
        });
        final String join = String.join(";",
                "newList=" + newList.size() + ",newCount=" + newCount.get(),
                "dirtyList=" + dirtyList.size() + ",dirtyCount=" + dirtyCount.get(),
                "deleteList=" + deleteList.size() + ",deleteCount=" + deleteCount.get()
        );
        log.info(join);
    }
}
