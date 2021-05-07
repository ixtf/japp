package com.github.ixtf.mongo;

import com.mongodb.DBRef;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;

import java.io.Serializable;

import static com.github.ixtf.mongo.Jmongo.collectionName;

public abstract class MongoEntityBase implements Serializable {
    @BsonId
    @Getter
    @Setter
    protected String id;
    @Getter
    @Setter
    protected boolean deleted;

    public DBRef toDBRef() {
        return toDBRef(collectionName(getClass()));
    }

    public DBRef toDBRef(final String collectionName) {
        return new DBRef(collectionName, id);
    }

    public DBRef toDBRef(final String databaseName, final String collectionName) {
        return new DBRef(databaseName, collectionName, id);
    }

}
