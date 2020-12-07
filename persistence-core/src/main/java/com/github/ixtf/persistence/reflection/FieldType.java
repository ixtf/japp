package com.github.ixtf.persistence.reflection;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * @author jzb 2019-02-14
 */
public enum FieldType {
    SUBENTITY, EMBEDDABLE, MAP, COLLECTION, DEFAULT;

    public static FieldType of(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            return COLLECTION;
        }
        if (Map.class.isAssignableFrom(field.getType())) {
            return MAP;
        }
        if (field.getType().isAnnotationPresent(Embeddable.class)) {
            return EMBEDDABLE;
        }
        if (field.getType().isAnnotationPresent(Entity.class)) {
            return SUBENTITY;
        }

        return DEFAULT;
    }

}
