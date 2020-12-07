package com.github.ixtf.persistence.api.reader;

import com.github.ixtf.persistence.api.ValueReader;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;

/**
 * @author jzb 2019-02-15
 */
public final class BigDecimalValueReader implements ValueReader {
    @Override
    public <T> boolean isCompatible(Class<T> clazz) {
        return BigDecimal.class.equals(clazz);
    }

    @Override
    public <T> T read(Class<T> clazz, Object value) {
        return ((T) (value instanceof BigDecimal ? value : NumberUtils.createBigDecimal(value.toString())));
    }
}
