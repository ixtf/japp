package com.github.ixtf.persistence.reflection;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * @author jzb 2019-02-14
 */
public interface FieldRepresentation extends Serializable {

    static FieldRepresentationBuilder builder() {
        return new FieldRepresentationBuilder();
    }

    FieldType getType();

    /**
     * {@link Field}
     *
     * @return the field
     */
    Field getNativeField();

    /**
     * {@link Column#name()}
     *
     * @return the name
     */
    String getColName();

    /**
     * @return The Java Field name {@link Field#getName()}
     */
    String getFieldName();

    Class getFieldType();

    boolean isId();

    /**
     *
     * @param <T> 类型
     * @return 自定义转换
     */
    <T extends AttributeConverter> Optional<Class<? extends AttributeConverter>> getConverter();

}

