package com.github.ixtf.persistence.api.reader;


import com.github.ixtf.persistence.api.ValueReader;

import java.util.Date;

/**
 * @author jzb 2019-02-15
 */
public final class DateValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Date.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (Date.class.isInstance(value)) {
            return (T) value;
        }

        if (Number.class.isInstance(value)) {
            return (T) new Date(((Number) value).longValue());
        }

        return (T) new Date(value.toString());
    }
}
