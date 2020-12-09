package com.github.ixtf.persistence.api;

import java.util.List;
import java.util.ServiceLoader;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * @author jzb 2019-02-15
 */
public final class ValueWriterDecorator implements ValueWriter {
    private static final ValueWriterDecorator INSTANCE = new ValueWriterDecorator();
    private static final List<ValueWriter> writers = ServiceLoader.load(ValueWriter.class)
            .stream().parallel()
            .map(ServiceLoader.Provider::get)
            .collect(toUnmodifiableList());

    private ValueWriterDecorator() {
    }

    public static ValueWriterDecorator getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isCompatible(Class clazz) {
        return writers.parallelStream().anyMatch(writerField -> writerField.isCompatible(clazz));
    }

    @Override
    public Object write(Object object) {
        if (object == null) {
            return null;
        }
        final var clazz = object.getClass();
        final var valueWriter = writers.parallelStream()
                .filter(r -> r.isCompatible(clazz))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("类型[" + clazz + "]不支持"));
        return valueWriter.write(object);
    }

}
