package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;
import com.google.common.collect.Lists;

import java.util.EnumSet;
import java.util.List;

/**
 * @author jzb 2019-02-15
 */
public final class EnumValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return Enum.class.isAssignableFrom(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (clazz.isInstance(value)) {
            return (T) value;
        }

        final List<Enum> elements = getEnumList((Class<Enum>) clazz);
        if (Number.class.isInstance(value)) {
            final int index = Number.class.cast(value).intValue();
            return (T) elements.stream().filter(element -> element.ordinal() == index).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("枚举index不存在: " + index));
        }
        final String name = value.toString();
        return (T) elements.stream().filter(element -> element.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("枚举name不存在: " + name));
    }

    private List<Enum> getEnumList(Class<Enum> clazz) {
        EnumSet enumSet = EnumSet.allOf(clazz);
        return Lists.newArrayList(enumSet);
    }
}
