package com.github.ixtf.mongo;

import com.github.benmanes.caffeine.cache.Caffeine;

public abstract class CqrsRepositoryJmongo<T extends MongoEntityBase> extends BaseRepositoryJmongo<T> implements RepositoryJmongo<T> {

    protected Caffeine<Object, Object> _cacheBuilder() {
        return Caffeine.newBuilder();
    }

}
