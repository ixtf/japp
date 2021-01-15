package com.github.ixtf.persistence.mongo;

import com.github.ixtf.persistence.api.AbstractEntityConverter;
import com.github.ixtf.persistence.api.EntityConverter;
import com.github.ixtf.persistence.api.ValueWriterDecorator;
import com.github.ixtf.persistence.reflection.ClassRepresentation;
import com.github.ixtf.persistence.reflection.ClassRepresentations;
import com.github.ixtf.persistence.reflection.FieldRepresentation;
import com.github.ixtf.persistence.reflection.GenericFieldRepresentation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.IterableUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.stream.StreamSupport;

import static com.github.ixtf.persistence.mongo.Jmongo.ID_COL;
import static java.util.stream.Collectors.toList;

/**
 * @author jzb 2019-02-18
 */
@Slf4j
public class DocumentEntityConverter extends AbstractEntityConverter {
    private final Jmongo jmongo;

    private DocumentEntityConverter(Jmongo jmongo) {
        this.jmongo = jmongo;
    }

    static EntityConverter get(Jmongo jmongo) {
        return new DocumentEntityConverter(jmongo);
    }

    @Override
    protected Object getColValue(FieldRepresentation fieldRepresentation, Object dbValue) {
        final var document = (Document) dbValue;
        final var colName = fieldRepresentation.isId() ? ID_COL : fieldRepresentation.getColName();
        return document.get(colName);
    }

    @Override
    protected Object convertToEntityAttribute_SubEntity(Object entity, FieldRepresentation fieldRepresentation, Object colValue) {
        final var subEntityClass = fieldRepresentation.getFieldType();
        if (colValue instanceof String || colValue instanceof ObjectId) {
            final LazyLoader lazyLoader = () -> {
                if (log.isDebugEnabled()) {
                    log.debug("lazyLoaderSubEntity[" + entity.getClass().getSimpleName() + "," + fieldRepresentation.getFieldName() + "]");
                }
                return jmongo.find(subEntityClass, colValue).block();
            };
            final var enhancer = new Enhancer();
            enhancer.setSuperclass(subEntityClass);
            return enhancer.create(subEntityClass, lazyLoader);
        }
        return toEntity(subEntityClass, colValue);
    }

    @Override
    protected Object convertToEntityAttribute_Embeddable(Object entity, FieldRepresentation fieldRepresentation, Object colValue) {
        return toEntity(fieldRepresentation.getFieldType(), colValue);
    }

    @Override
    protected Object convertToEntityAttribute_Collection_Entity(Object entity, GenericFieldRepresentation fieldRepresentation, Iterable iterable) {
        if (IterableUtils.isEmpty(iterable)) {
            return null;
        }
        final var elementType = fieldRepresentation.getElementType();
        final var collector = fieldRepresentation.getCollector();
        final var itemValue = IterableUtils.get(iterable, 0);
        if (itemValue instanceof String || itemValue instanceof ObjectId) {
            final LazyLoader lazyLoader = () -> {
                if (log.isDebugEnabled()) {
                    log.debug("lazyLoaderEntity_Collection[" + entity.getClass().getSimpleName() + "." + fieldRepresentation.getFieldName() + "]");
                }
                return jmongo.find(elementType, Flux.fromIterable(iterable)).collect(collector).block();
            };
            final var enhancer = new Enhancer();
            final var rawType = fieldRepresentation.getRawType();
            enhancer.setSuperclass(rawType);
            return enhancer.create(rawType, lazyLoader);
        }
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(it -> toEntity(elementType, it))
                .collect(collector);
    }

    @Override
    protected Object convertToDatabaseColumn_Collection(Object entity, GenericFieldRepresentation fieldRepresentation, Iterable iterable) {
        if (IterableUtils.isEmpty(iterable)) {
            return null;
        }
        final Function function;
        if (fieldRepresentation.isEntityField()) {
            final ClassRepresentation<?> elementClassRepresentation = ClassRepresentations.create(fieldRepresentation.getElementType());
            final var idFieldName = elementClassRepresentation.getId().map(FieldRepresentation::getFieldName).get();
            function = element -> {
                try {
                    return PropertyUtils.getProperty(element, idFieldName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } else if (fieldRepresentation.isEmbeddableField()) {
            function = element -> toDbData(new Document(), element);
        } else {
            function = ValueWriterDecorator.getInstance()::write;
        }
        return StreamSupport.stream(iterable.spliterator(), false).map(function).collect(toList());
    }

    @Override
    protected Object convertToDatabaseColumn_Embeddable(Object entity, FieldRepresentation fieldRepresentation, Object fieldValue) {
        return toDbData(new Document(), fieldValue);
    }

    @SneakyThrows
    @Override
    protected Object convertToDatabaseColumn_SubEntity(Object entity, FieldRepresentation fieldRepresentation, Object fieldValue) {
        final ClassRepresentation<?> fieldClassRepresentation = ClassRepresentations.create(fieldRepresentation.getFieldType());
        final var idFieldName = fieldClassRepresentation.getId().map(FieldRepresentation::getFieldName).get();
        return PropertyUtils.getProperty(fieldValue, idFieldName);
    }

    @Override
    protected void setDatabaseColumnValue(Object entity, FieldRepresentation fieldRepresentation, Object dbData, Object colValue) {
        final var document = (Document) dbData;
        final var colName = fieldRepresentation.isId() ? ID_COL : fieldRepresentation.getColName();
        final var valueWrite = ValueWriterDecorator.getInstance();
        if (valueWrite.isCompatible(fieldRepresentation.getFieldType())) {
            final var value = valueWrite.write(colValue);
            document.append(colName, value);
            return;
        }
        document.append(colName, colValue);
    }

}
