package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.data.EntityDTO;
import org.apache.commons.lang3.Validate;
import reactor.core.publisher.Flux;

public interface RepositoryJmongo<T extends MongoEntityBase> {
    /**
     * 常驻内存
     *
     * @param id 根
     * @return
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

    T find(String id);

    T fetch(String id);

    void delete(T t);

    Flux<T> list();

    boolean exists(String id);

    default boolean exists(T entity) {
        return exists(entity.getId());
    }

    default T find(EntityDTO o) {
        return find(o.getId());
    }

    default void delete(String id) {
        delete(find(id));
    }

    default void delete(EntityDTO o) {
        delete(find(o));
    }

    default T getOrCreate(String id) {
        Validate.notBlank(id);
        final var entity = exists(id) ? find(id) : create();
        if (J.nonBlank(id)) {
            entity.setId(id);
        }
        return entity;
    }

    default void save(T entity) {
        if (exists(entity)) {
            update(entity);
        } else {
            insert(entity);
        }
    }
}
