package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.persistence.IEntity;
import com.github.ixtf.persistence.runtime.AbstractUnitOfWork;
import com.mongodb.reactivestreams.client.MongoCollection;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;
import reactor.core.publisher.Mono;

import static com.github.ixtf.mongo.Jmongo.ID_COL;

/**
 * @author jzb 2019-02-18
 */
@Slf4j
public class MongoUnitOfWork extends AbstractUnitOfWork {
    private final Jmongo jmongo;
    private boolean committed;

    MongoUnitOfWork(Jmongo jmongo) {
        this.jmongo = jmongo;
    }

    @Override
    protected boolean exists(IEntity o) {
        return jmongo.exists(o);
    }

    @Override
    synchronized public MongoUnitOfWork commit() {
        if (!committed) {
            try {
                handleNewList();
                handleDirtyList();
                handleDeleteList();
            } finally {
                committed = true;
            }
        }
        return this;
    }

    private void handleNewList() {
        for (final var entity : newList) {
            final MongoCollection collection = jmongo.entityCollection(entity.getClass());
            callbackStream(entity, PrePersist.class).forEach(it -> it.callback(entity));
            if (J.isBlank(entity.getId())) {
                entity.setId(new ObjectId().toHexString());
            }
            Mono.from(collection.insertOne(entity)).block();
            callbackStream(entity, PostPersist.class).forEach(it -> it.callback(entity));
        }
    }

    private void handleDirtyList() {
        for (final var entity : dirtyList) {
            final MongoCollection collection = jmongo.entityCollection(entity.getClass());
            callbackStream(entity, PreUpdate.class).forEach(it -> it.callback(entity));
            final var document = jmongo.toBsonDocument(entity);
            final var id = document.get(ID_COL);
            final var condition = new BsonDocument().append(ID_COL, id);
            Mono.from(collection.replaceOne(condition, entity)).block();
            callbackStream(entity, PostUpdate.class).forEach(it -> it.callback(entity));
        }
    }

    private void handleDeleteList() {
        for (final var entity : deleteList) {
            final MongoCollection collection = jmongo.entityCollection(entity.getClass());
            callbackStream(entity, PreRemove.class).forEach(it -> it.callback(entity));
            final var document = jmongo.toBsonDocument(entity);
            final var id = document.get(ID_COL);
            final var condition = new BsonDocument().append(ID_COL, id);
            Mono.from(collection.deleteOne(condition)).block();
            callbackStream(entity, PostRemove.class).forEach(it -> it.callback(entity));
        }
    }

    @Override
    synchronized public MongoUnitOfWork rollback() {
        // todo mongo 需要集群才支持事务，后续实现
        return this;
    }

}
