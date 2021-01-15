package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

/**
 * @author jzb 2019-02-15
 */
public final class ByteValueReader implements ValueReader {

    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Byte.class.equals(clazz) || byte.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (Byte.class.isInstance(value)) {
            return (T) value;
        }
        if (Number.class.isInstance(value)) {
            return (T) Byte.valueOf(Number.class.cast(value).byteValue());
        } else {
            return (T) Byte.valueOf(value.toString());
        }
    }
}
