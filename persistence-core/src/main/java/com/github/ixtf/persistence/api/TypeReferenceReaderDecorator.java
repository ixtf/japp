package com.github.ixtf.persistence.api;

import com.google.common.collect.Lists;

import java.lang.reflect.Type;
import java.util.List;
import java.util.ServiceLoader;

public final class TypeReferenceReaderDecorator implements TypeReferenceReader {
    private static final TypeReferenceReaderDecorator INSTANCE = new TypeReferenceReaderDecorator();
    private final List<TypeReferenceReader> readers = Lists.newArrayList();

    {
        ServiceLoader.load(TypeReferenceReader.class).forEach(readers::add);
    }

    public static TypeReferenceReaderDecorator getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isCompatible(Type type) {
        return readers.stream().anyMatch(r -> r.isCompatible(type));
    }

    @Override
    public <T> T convert(Type type, Object value) {
        final TypeReferenceReader valueReader = readers.stream().filter(r -> r.isCompatible(type)).findFirst().
                orElseThrow(() -> new UnsupportedOperationException("Type[" + type + "]不支持"));
        return valueReader.convert(type, value);
    }

}
