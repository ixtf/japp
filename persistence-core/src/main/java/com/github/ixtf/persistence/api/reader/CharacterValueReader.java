package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;

import static java.lang.Character.MIN_VALUE;

/**
 * @author jzb 2019-02-15
 */
public final class CharacterValueReader implements ValueReader {

    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Character.class.equals(clazz) || char.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (Character.class.isInstance(value)) {
            return (T) value;
        }
        if (Number.class.isInstance(value)) {
            return (T) Character.valueOf((char) Number.class.cast(value).intValue());
        }

        if (value.toString().isEmpty()) {
            return (T) Character.valueOf(MIN_VALUE);
        }
        return (T) Character.valueOf(value.toString().charAt(0));
    }


}
