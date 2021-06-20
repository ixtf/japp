package com.github.ixtf.mongo;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.types.ObjectId;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;

public abstract class BaseRepositoryJmongo<T extends MongoEntityBase> implements RepositoryJmongo<T> {
    private final Class<T> entityClass = _entityClass();
    @Inject
    private Jmongo jmongo;
    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final LoadingCache<String, T> cache = _cache();

    @SneakyThrows({NoSuchMethodException.class, InvocationTargetException.class, InstantiationException.class, IllegalAccessException.class})
    @Override
    public T create() {
        final var entity = entityClass.getDeclaredConstructor().newInstance();
        entity.setId(ObjectId.get().toHexString());
        return entity;
    }

    @Override
    public void insert(T entity) {
        jmongo.uow().registerNew(entity).commit();
        getCache().put(entity.getId(), entity);
    }

    @Override
    public void update(T entity) {
        jmongo.uow().registerDirty(entity).commit();
        getCache().put(entity.getId(), entity);
    }

    @Override
    public T find(String id) {
        return getCache().get(id);
    }

    @Override
    public void delete(T t) {
        synchronized (t) {
            t.setDeleted(true);
        }
        jmongo.uow().registerDirty(t).commit();
        getCache().invalidate(t.getId());
    }

    @Override
    public boolean exists(String id) {
        return jmongo.exists(entityClass, id);
    }

    @Override
    public T fetch(String id) {
        return jmongo.find(entityClass, id).block();
    }

    protected LoadingCache<String, T> _cache() {
        return Caffeine.newBuilder().build(this::fetch);
    }

    private final Class<T> _entityClass() {
        final var parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
        return (Class<T>) parameterizedType.getActualTypeArguments()[0];
    }

}
