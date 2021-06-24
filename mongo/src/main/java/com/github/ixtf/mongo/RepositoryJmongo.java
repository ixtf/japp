package com.github.ixtf.mongo;

import com.github.ixtf.data.EntityDTO;
import reactor.core.publisher.Flux;

public interface RepositoryJmongo<T extends MongoEntityBase> {
    /**
     * 常驻内存
     *
     * @param id 根
     * @return entity
     */
    T build(String id);

    default T build(EntityDTO o) {
        return build(o.getId());
    }

    /**
     * 清理常驻内存，单个
     *
     * @param id 根
     */
    void invalidateBuild(String id);

    default void invalidateBuild(T entity) {
        invalidateBuild(entity.getId());
    }

    /**
     * 清理常驻内存
     */
    void invalidateBuild();

    T create();

    void insert(T entity);

    void update(T entity);

    default void save(T entity) {
        if (exists(entity)) {
            update(entity);
        } else {
            insert(entity);
        }
    }

    T find(String id);

    default T find(EntityDTO o) {
        return find(o.getId());
    }

    T fetch(String id);

    default T fetch(EntityDTO o) {
        return fetch(o.getId());
    }

    void delete(T t);

    default void delete(String id) {
        delete(find(id));
    }

    default void delete(EntityDTO o) {
        delete(find(o));
    }

    boolean exists(String id);

    default boolean exists(T entity) {
        return exists(entity.getId());
    }

    Flux<T> list();
}
