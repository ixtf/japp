package com.github.ixtf.mongo;

import com.github.ixtf.persistence.IEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.Objects;

import static com.github.ixtf.guice.GuiceModule.getInstance;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
public abstract class MongoEntityBase implements IEntity {
    @ToString.Include
    @EqualsAndHashCode.Include
    @BsonId
    protected String id;
    protected boolean deleted;

    public JmongoRef toRef() {
        final var jmongo = getInstance(Jmongo.class);
        final var collection = jmongo.collection(getClass());
        final var namespace = collection.getNamespace();
        final var databaseName = namespace.getDatabaseName();
        final var collectionName = namespace.getCollectionName();
        if (Objects.equals(jmongo.database().getName(), databaseName)) {
            return new JmongoRef(null, collectionName, getId());
        }
        return new JmongoRef(databaseName, collectionName, getId());
    }
}
