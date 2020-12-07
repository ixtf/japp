package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

/**
 * @author jzb 2019-02-15
 */
public final class LongValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Long.class.equals(clazz) || long.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (Long.class.isInstance(value)) {
            return (T) value;
        }
        if (Number.class.isInstance(value)) {
            return (T) Long.valueOf(Number.class.cast(value).longValue());
        } else {
            return (T) Long.valueOf(value.toString());
        }
    }
}
