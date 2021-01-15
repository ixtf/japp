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
        parameterizedType = ParameterizedType.class.cast(getNativeField().getGenericType());
        rawType = Class.class.cast(parameterizedType.getRawType());
        // fixme Set<TreeSet<elementType>> 情况下会报错
        elementType = getActualType(parameterizedType.getActualTypeArguments()[0]);
        entityField = elementType.getAnnotation(Entity.class) != null;
        embeddableField = elementType.getAnnotation(Embeddable.class) != null;
    }

    private Class getActualType(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType pType = (ParameterizedType) type;
            return getActualType(pType.getActualTypeArguments()[0]);
        }
        return Class.class.cast(type);
    }

    @Override
    public boolean isId() {
        return false;
    }

    public Collector getCollector() {
        Class<?> type = getNativeField().getType();
        if (Deque.class.equals(type) || Queue.class.equals(type)) {
            return Collectors.toCollection(LinkedList::new);
        } else if (List.class.isAssignableFrom(type) || Collection.class.equals(type) || Iterable.class.equals(type)) {
            return Collectors.toCollection(ArrayList::new);
        } else if (NavigableSet.class.equals(type) || SortedSet.class.equals(type)) {
            return Collectors.toCollection(TreeSet::new);
        } else if (Set.class.isAssignableFrom(type)) {
            final Type genericType = getNativeField().getGenericType();
            if (genericType instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) genericType;
                final Type subType = parameterizedType.getActualTypeArguments()[0];
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
        final StringBuilder sb = new StringBuilder("GenericFieldRepresentation{");
        sb.append(", type=").append(type);
        sb.append(", field=").append(nativeField);
        sb.append(", name='").append(colName).append('\'');
        sb.append(", fieldName='").append(fieldName).append('\'');
        sb.append(", converter=").append(converter);
        sb.append('}');
        return sb.toString();
    }
}
