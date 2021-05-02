package com.github.ixtf.persistence.reflection;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class GenericFieldRepresentation extends AbstractFieldRepresentation {
    @Getter
    private final ParameterizedType parameterizedType;
    @Getter
    private final Class rawType;
    @Getter
    private final Class elementType;
    @Getter
    private final boolean entityField;
    @Getter
    private final boolean embeddableField;

    GenericFieldRepresentation(FieldType type, Field field, String name, Class<? extends AttributeConverter> converter) {
        super(type, name, field, converter);
        parameterizedType = (ParameterizedType) getNativeField().getGenericType();
        rawType = (Class) parameterizedType.getRawType();
        // fixme Set<TreeSet<elementType>> 情况下会报错
        elementType = getActualType(parameterizedType.getActualTypeArguments()[0]);
        entityField = elementType.getAnnotation(Entity.class) != null;
        embeddableField = elementType.getAnnotation(Embeddable.class) != null;
    }

    private Class getActualType(Type type) {
        if (type instanceof ParameterizedType pType) {
            return getActualType(pType.getActualTypeArguments()[0]);
        }
        return (Class) type;
    }

    @Override
    public boolean isId() {
        return false;
    }

    public Collector getCollector() {
        final var type = getNativeField().getType();
        if (Deque.class.equals(type) || Queue.class.equals(type)) {
            return Collectors.toCollection(LinkedList::new);
        } else if (List.class.isAssignableFrom(type) || Collection.class.equals(type) || Iterable.class.equals(type)) {
            return Collectors.toCollection(ArrayList::new);
        } else if (NavigableSet.class.equals(type) || SortedSet.class.equals(type)) {
            return Collectors.toCollection(TreeSet::new);
        } else if (Set.class.isAssignableFrom(type)) {
            final var genericType = getNativeField().getGenericType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                final var subType = parameterizedType.getActualTypeArguments()[0];
                if (subType instanceof TreeSet) {

                }
            }
            return Collectors.toCollection(HashSet::new);
        }
        throw new UnsupportedOperationException("This collection is not supported yet: " + type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenericFieldRepresentation that = (GenericFieldRepresentation) o;
        return type == that.type &&
                Objects.equals(nativeField, that.nativeField) &&
                Objects.equals(colName, that.colName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, nativeField, colName);
    }

    @Override
    public String toString() {
        return new StringBuilder("GenericFieldRepresentation{")
                .append(", type=").append(type)
                .append(", field=").append(nativeField)
                .append(", name='").append(colName).append('\'')
                .append(", fieldName='").append(fieldName).append('\'')
                .append(", converter=").append(converter)
                .append('}')
                .toString();
    }
}
