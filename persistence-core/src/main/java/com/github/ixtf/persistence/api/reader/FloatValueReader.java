package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

/**
 * @author jzb 2019-02-15
 */
public final class FloatValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Float.class.equals(clazz) || float.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (Float.class.isInstance(value)) {
            return (T) value;
        }
        if (Number.class.isInstance(value)) {
            return (T) Float.valueOf(Number.class.cast(value).floatValue());
        } else {
            return (T) Float.valueOf(value.toString());
        }
    }
}
