package com.github.ixtf.persistence.api;

import java.lang.reflect.Type;

/**
 * @author jzb 2019-02-15
 */
public interface TypeReferenceReader {

    boolean isCompatible(Type clazz);

    <T> T convert(Type type, Object value);

}
