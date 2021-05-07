package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.persistence.IEntity;
import com.github.ixtf.persistence.runtime.AbstractUnitOfWork;
import com.google.common.collect.Lists;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ixtf.mongo.Jmongo.ID_COL;

/**
 * @author jzb 2019-02-18
 */
@Slf4j
public class MongoUnitOfWork extends AbstractUnitOfWork {
    private final Jmongo jmongo;
    private final List<BulkWriteResult> commitResult = Lists.newArrayList();

    MongoUnitOfWork(Jmongo jmongo) {
        this.jmongo = jmongo;
    }

    @Override
    protected boolean exists(IEntity o) {
        return jmongo.exists(o);
    }

    @Override
    synchronized public MongoUnitOfWork commit() {
        handleNewList();
        handleDirtyList();
        handleDeleteList();
        return this;
    }

    private void handleNewList() {
        for (final var entity : newList) {
            final MongoCollection collection = jmongo.entityCollection(entity.getClass());
            callbackStream(entity, PrePersist.class).forEach(it -> it.callback(entity));
            if (J.isBlank(entity.getId())) {
                entity.setId(new ObjectId().toHexString());
            }
            collection.insertOne(entity);
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
            collection.replaceOne(condition, entity);
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
            collection.deleteOne(condition);
            callbackStream(entity, PostRemove.class).forEach(it -> it.callback(entity));
        }
    }

    @Override
    synchronized public MongoUnitOfWork rollback() {
        // todo mongo 需要集群才支持事务，后续实现
        return this;
    }

    private void log(Iterable<BulkWriteResult> bulkWriteResults) {
        final var newCount = new AtomicInteger();
        final var dirtyCount = new AtomicInteger();
        final var deleteCount = new AtomicInteger();
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
