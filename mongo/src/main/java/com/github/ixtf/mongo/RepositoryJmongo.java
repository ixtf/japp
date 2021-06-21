package com.github.ixtf.mongo;

import com.github.ixtf.J;
import com.github.ixtf.data.EntityDTO;
import reactor.core.publisher.Flux;

public interface RepositoryJmongo<T extends MongoEntityBase> {

    T create();

    void insert(T entity);

    void update(T entity);

    T find(String id);

    T fetch(String id);

    void delete(T t);

    Flux<T> list();

    boolean exists(String id);

    default T find(EntityDTO o) {
        return find(o.getId());
    }

    default T fetch(EntityDTO o) {
        return fetch(o.getId());
    }

    default void delete(String id) {
        delete(find(id));
    }

    default void delete(EntityDTO o) {
        delete(find(o));
    }

    default T getOrCreate(String id) {
        final var entity = exists(id) ? find(id) : create();
        if (J.nonBlank(id)) {
            entity.setId(id);
        }
        return entity;
    }

    default void save(T entity) {
        if (exists(entity.getId())) {
            update(entity);
        } else {
            insert(entity);
        }
    }
}
