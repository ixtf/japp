package com.github.ixtf.persistence.reflection;

import jakarta.persistence.AttributeConverter;

import java.lang.reflect.Field;

class FieldRepresentationBuilder {
    private FieldType type;
    private Field field;
    private String colName;
    private String collectionName;
    private Class<? extends AttributeConverter> converter;
    private boolean id;

    public FieldRepresentationBuilder withType(FieldType type) {
        this.type = type;
        return this;
    }

    public FieldRepresentationBuilder withField(Field field) {
        this.field = field;
        return this;
    }

    public FieldRepresentationBuilder withColName(String colName) {
        this.colName = colName;
        return this;
    }

    public FieldRepresentationBuilder withEntityName(String entityName) {
        this.collectionName = entityName;
        return this;
    }

    public FieldRepresentationBuilder withConverter(Class<? extends AttributeConverter> converter) {
        this.converter = converter;
        return this;
    }

    public FieldRepresentationBuilder withId(boolean id) {
        this.id = id;
        return this;
    }

    public DefaultFieldRepresentation buildDefault() {
        return new DefaultFieldRepresentation(type, field, colName, converter, id);
    }

    public GenericFieldRepresentation buildGeneric() {
        return new GenericFieldRepresentation(type, field, colName, converter);
    }

    public EmbeddedFieldRepresentation buildEmedded() {
        return new EmbeddedFieldRepresentation(type, field, colName, collectionName);
    }

}
