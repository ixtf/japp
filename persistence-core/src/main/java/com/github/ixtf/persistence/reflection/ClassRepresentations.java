package com.github.ixtf.persistence.reflection;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.ixtf.J;
import org.apache.commons.lang3.reflect.FieldUtils;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * @author jzb 2019-02-14
 */
public final class ClassRepresentations {
    private static final LoadingCache<Class, ClassRepresentation> cache = Caffeine.newBuilder().build(entityClass -> {
        final var constructor = makeAccessible(entityClass);
        final var tableName = tableName(entityClass);
        final var fields = FieldUtils.getAllFieldsList(entityClass)
                .parallelStream()
                .filter(field -> {
                    final Id idAnnotation = field.getAnnotation(Id.class);
                    if (idAnnotation != null) {
                        return true;
                    }
                    if (Modifier.isStatic(field.getModifiers())) {
                        return false;
                    }
                    final Transient transientAnnotation = field.getAnnotation(Transient.class);
                    return transientAnnotation == null;
                })
                .map(ClassRepresentations::to)
                .collect(toUnmodifiableList());
        return new DefaultClassRepresentation(tableName, entityClass, constructor, fields);
    });
    private static final LoadingCache<Class<? extends AttributeConverter>, AttributeConverter> converterCache = Caffeine.newBuilder().build(clazz -> {
        final var constructor = makeAccessible(clazz);
        return (AttributeConverter) constructor.newInstance();
    });

    public static <T> ClassRepresentation<T> create(T o) {
        return create((Class<T>) o.getClass());
    }

    public static <T> ClassRepresentation<T> create(Class<T> entityClass) {
        return cache.get(J.actualClass(entityClass));
    }

    private static FieldRepresentation to(Field field) {
        final var fieldType = FieldType.of(field);
        final var idAnnotation = field.getAnnotation(Id.class);
        final var columnAnnotation = field.getAnnotation(Column.class);
        final var id = idAnnotation != null;
        final var columnName = id ? null : ofNullable(columnAnnotation).map(Column::name).filter(J::nonBlank).orElseGet(field::getName);
        final var builder = FieldRepresentation.builder()
                .withId(id)
                .withColName(columnName)
                .withField(field)
                .withType(fieldType);
        final var convert = field.getAnnotation(Convert.class);
        if (nonNull(convert)) {
            builder.withConverter(convert.converter());
        }
        switch (fieldType) {
            case COLLECTION:
            case MAP:
                return builder.buildGeneric();
            case EMBEDDABLE:
                return builder.withEntityName(tableName(field.getType())).buildEmedded();
            default:
                return builder.buildDefault();
        }
    }

    private static String tableName(@NotNull Class<?> clazz) {
        final var annotation = clazz.getAnnotation(Entity.class);
        if (annotation == null) {
            return null;
        }
        return ofNullable(annotation.name())
                .filter(J::nonBlank)
                .orElseGet(() -> {
                    final String simpleName = clazz.getSimpleName();
                    return "T_" + simpleName;
                });
    }

    private static Constructor makeAccessible(Class clazz) {
        final var constructors = Stream.of(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .collect(toList());
        if (constructors.isEmpty()) {
            throw new ConstructorException(clazz);
        }

        return constructors.stream()
                .filter(c -> Modifier.isPublic(c.getModifiers()))
                .findFirst()
                .orElseGet(() -> {
                    final var constructor = constructors.get(0);
                    constructor.setAccessible(true);
                    return constructor;
                });
    }
}
