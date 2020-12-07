package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jzb 2019-02-15
 */
public final class BooleanValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Boolean.class.equals(clazz) || AtomicBoolean.class.equals(clazz) || boolean.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        boolean isAtomicBoolean = AtomicBoolean.class.equals(clazz);
        if (isAtomicBoolean && AtomicBoolean.class.isInstance(value)) {
            return (T) value;
        }

        Boolean bool = null;
        if (Boolean.class.isInstance(value)) {
            bool = Boolean.class.cast(value);
        } else if (AtomicBoolean.class.isInstance(value)) {
            bool = AtomicBoolean.class.cast(value).get();
        } else if (Number.class.isInstance(value)) {
            bool = Number.class.cast(value).longValue() != 0;
        } else if (String.class.isInstance(value)) {
            bool = Boolean.valueOf(value.toString());
        }

        if (isAtomicBoolean) {
            return (T) new AtomicBoolean(bool);
        }

        return (T) bool;
    }


}
