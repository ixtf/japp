package com.github.ixtf.persistence.api;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.ixtf.persistence.reflection.ClassRepresentation;
import com.github.ixtf.persistence.reflection.ClassRepresentations;
import com.github.ixtf.persistence.reflection.FieldRepresentation;
import jakarta.persistence.AttributeConverter;

/**
 * @author jzb 2019-02-18
 */
public interface EntityConverter {
    LoadingCache<Class<? extends AttributeConverter>, AttributeConverter> CONVERTER_CACHE = Caffeine.newBuilder().build(clazz -> clazz.getConstructor().newInstance());

    static AttributeConverter attributeConverter(FieldRepresentation fieldRepresentation) {
        return fieldRepresentation.getConverter().map(CONVERTER_CACHE::get).orElse(null);
    }

    default <T> T toEntity(Class<T> entityClass, Object dbData) {
        final ClassRepresentation<T> classRepresentation = ClassRepresentations.create(entityClass);
        return toEntity(classRepresentation, dbData);
    }

    <T> T toEntity(ClassRepresentation<T> entityClass, Object dbData);

    void fillEntity(Object entityClass, Object dbData);

    <DB> DB toDbData(DB dbData, Object entityInstance);
}
