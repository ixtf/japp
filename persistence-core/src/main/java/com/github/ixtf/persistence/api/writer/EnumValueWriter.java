package com.github.ixtf.persistence.api.writer;

import com.github.ixtf.persistence.api.ValueWriter;

/**
 * @author jzb 2019-02-15
 */
public class EnumValueWriter implements ValueWriter<Enum<?>, String> {

    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Enum.class.isAssignableFrom(clazz);
    }

    @Override
    public String write(Enum<?> object) {
        return object.name();
    }
}
