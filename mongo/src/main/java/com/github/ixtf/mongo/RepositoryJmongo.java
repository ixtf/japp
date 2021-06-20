package com.github.ixtf.mongo;

import com.github.ixtf.data.EntityDTO;
import reactor.core.publisher.Flux;

import static java.util.Optional.ofNullable;

public interface RepositoryJmongo<T extends MongoEntityBase> {

    T create();

    void insert(T entity);

    void update(T entity);

    T find(String id);

    Flux<T> list();

    void delete(T t);

    boolean exists(String id);

    T fetch(String id);

    default T find(EntityDTO o) {
        return ofNullable(o).map(EntityDTO::getId).map(this::find).orElse(null);
    }

    default T find(JmongoRef o) {
        return ofNullable(o).map(JmongoRef::getId).map(this::find).orElse(null);
    }

    default void delete(String id) {
        delete(find(id));
    }

    default void delete(EntityDTO o) {
        delete(find(o));
    }

    default void delete(JmongoRef o) {
        delete(find(o));
    }

    default T getOrCreate(String id) {
        return exists(id) ? find(id) : create();
    }

    default void save(T entity) {
        if (exists(entity.getId())) {
            update(entity);
        } else {
            insert(entity);
        }
    }
}
