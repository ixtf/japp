package com.github.ixtf.mongo;

import com.github.ixtf.persistence.IEntity;
import com.mongodb.DBRef;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;

import static com.github.ixtf.guice.GuiceModule.getInstance;

public abstract class MongoEntityBase implements IEntity {
    @BsonId
    @Getter
    @Setter
    protected String id;
    @Getter
    @Setter
    protected boolean deleted;

    public DBRef toDBRef() {
        final var jmongo = getInstance(Jmongo.class);
        final var collection = jmongo.collection(getClass());
        final var namespace = collection.getNamespace();
        final var databaseName = namespace.getDatabaseName();
        final var collectionName = namespace.getCollectionName();
        return new DBRef(databaseName, collectionName, getId());
    }

}
