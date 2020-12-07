package com.github.ixtf.persistence.api.writer;

import com.github.ixtf.persistence.api.ValueWriter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jzb 2019-02-15
 */
public class DefaultValueWriter implements ValueWriter<Object, Object> {

    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return String.class.isAssignableFrom(clazz)
                || BigDecimal.class.isAssignableFrom(clazz)
                || Date.class.isAssignableFrom(clazz)
                || short.class.isAssignableFrom(clazz)
                || Short.class.isAssignableFrom(clazz)
                || int.class.isAssignableFrom(clazz)
                || Integer.class.isAssignableFrom(clazz)
                || float.class.isAssignableFrom(clazz)
                || Float.class.isAssignableFrom(clazz)
                || long.class.isAssignableFrom(clazz)
                || Long.class.isAssignableFrom(clazz);
    }

    @Override
    public Object write(Object object) {
        return object;
    }
}
