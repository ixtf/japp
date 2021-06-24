package com.github.ixtf.mongo;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;

import static java.util.Optional.ofNullable;

public abstract class BaseRepositoryJmongo<T extends MongoEntityBase> implements RepositoryJmongo<T> {
    @Getter(lazy = true, value = AccessLevel.PROTECTED)
    private final LoadingCache<String, T> cache = _cache();
    protected final Class<T> entityClass = _entityClass();
    @Inject
    protected Jmongo jmongo;

    @Override
    public T build(String id) {
        return ofNullable(getCache().get(id)).orElseGet(() -> {
            final var entity = create();
            entity.setId(id);
            return entity;
        });
    }

    @Override
    public void invalidateBuild(String id) {
        getCache().invalidate(id);
    }

    @Override
    public void invalidateBuild() {
        getCache().invalidateAll();
    }

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
        if (entity.isDeleted()) {
            getCache().invalidate(entity.getId());
        } else {
            getCache().put(entity.getId(), entity);
        }
    }

    // fixme
    @Override
    public T find(String id) {
        return getCache().get(id);
    }

    @Override
    public Flux<T> list() {
        return jmongo.list(entityClass);
    }

    @Override
    public void delete(T entity) {
        entity.setDeleted(true);
        jmongo.uow().registerDirty(entity).commit();
        invalidateBuild(entity);
    }

    /**
     * 真正删除
     *
     * @param entity entity
     */
    protected void _delete(T entity) {
        jmongo.uow().registerDelete(entity).commit();
        invalidateBuild(entity);
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
