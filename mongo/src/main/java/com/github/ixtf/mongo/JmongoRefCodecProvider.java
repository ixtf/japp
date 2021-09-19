package com.github.ixtf.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class JmongoRefCodecProvider implements CodecProvider {

    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (clazz == JmongoRef.class) {
            return (Codec<T>) new JmongoRefCodec(registry);
        }
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
