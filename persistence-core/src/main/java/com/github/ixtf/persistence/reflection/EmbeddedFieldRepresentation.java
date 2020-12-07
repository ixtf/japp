package com.github.ixtf.persistence.reflection;

import java.lang.reflect.Field;
import java.util.Objects;

public final class EmbeddedFieldRepresentation extends AbstractFieldRepresentation {

    private final String entityName;

    public EmbeddedFieldRepresentation(FieldType type, Field field, String name, String entityName) {
        super(type, name, field, null);
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    @Override
    public boolean isId() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmbeddedFieldRepresentation that = (EmbeddedFieldRepresentation) o;
        return type == that.type &&
                Objects.equals(nativeField, that.nativeField) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(colName, that.colName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, nativeField, colName, entityName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EmbeddedFieldRepresentation{");
        sb.append("entityName='").append(entityName).append('\'');
        sb.append(", type=").append(type);
        sb.append(", field=").append(nativeField);
        sb.append(", name='").append(colName).append('\'');
        sb.append(", fieldName='").append(fieldName).append('\'');
        sb.append(", converter=").append(converter);
        sb.append('}');
        return sb.toString();
    }
}
