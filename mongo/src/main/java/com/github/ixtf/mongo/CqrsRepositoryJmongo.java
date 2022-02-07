package com.github.ixtf.mongo;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import static java.util.Optional.ofNullable;

public abstract class CqrsRepositoryJmongo<T extends MongoEntityBase>
    extends BaseRepositoryJmongo<T> implements RepositoryJmongo<T> {
  @Getter(lazy = true, value = AccessLevel.PROTECTED)
  private final LoadingCache<String, T> cache = _cacheBuilder().build(this::find);

  @Override
  public T build(String id) {
    Validate.notBlank(id);
    return ofNullable(getCache().get(id))
        .orElseGet(
            () -> {
              final var entity = create();
              entity.setId(id);
              return entity;
            });
  }

  protected abstract Caffeine<Object, Object> _cacheBuilder();
}
