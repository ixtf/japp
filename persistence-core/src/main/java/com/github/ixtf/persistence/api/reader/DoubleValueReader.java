package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

/**
 * @author jzb 2019-02-15
 */
public final class DoubleValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Double.class.equals(clazz) || double.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (Double.class.isInstance(value)) {
            return (T) value;
        }

        if (Number.class.isInstance(value)) {
            return (T) Double.valueOf(Number.class.cast(value).doubleValue());
        } else {
            return (T) Double.valueOf(value.toString());
        }
    }
}
