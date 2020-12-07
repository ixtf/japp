package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

/**
 * @author jzb 2019-02-15
 */
public final class StringValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return CharSequence.class.equals(clazz) || String.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (CharSequence.class.equals(clazz) && CharSequence.class.isInstance(value)) {
            return (T) value;
        }
        return (T) value.toString();
    }

}
