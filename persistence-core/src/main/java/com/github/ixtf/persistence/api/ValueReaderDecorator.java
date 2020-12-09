package com.github.ixtf.persistence.api;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public final class ValueReaderDecorator implements ValueReader {
    private static final ValueReaderDecorator INSTANCE = new ValueReaderDecorator();
    private static final List<ValueReader> readers = ServiceLoader.load(ValueReader.class)
            .stream().parallel()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableList());

    private ValueReaderDecorator() {
    }

    public static ValueReaderDecorator getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isCompatible(Class clazz) {
        return readers.parallelStream().anyMatch(r -> r.isCompatible(clazz));
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        final var valueReader = readers.parallelStream()
                .filter(r -> r.isCompatible(clazz))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("类型[" + clazz + "]不支持"));
        return valueReader.read(clazz, value);
    }

}