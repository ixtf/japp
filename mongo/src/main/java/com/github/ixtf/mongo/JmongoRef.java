package com.github.ixtf.mongo;

import lombok.Getter;
import org.apache.commons.lang3.Validate;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Objects;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static java.util.Optional.ofNullable;

public final class JmongoRef implements Serializable {
    @Getter
    private final String id;
    @Getter
    private final String collectionName;
    @Getter
    private final String databaseName;

    JmongoRef(final String databaseName, final String collectionName, final String id) {
        this.id = Validate.notBlank(id, "id");
        this.collectionName = Validate.notBlank(collectionName, "collectionName");
        this.databaseName = databaseName;
    }

    public <T extends MongoEntityBase> Mono<T> toEntity(Class<T> clazz) {
        final var jmongo = getInstance(Jmongo.class);
        final var client = jmongo.client();
        final var database = ofNullable(databaseName).map(client::getDatabase).orElseGet(jmongo::database);
        final var collection = database.getCollection(collectionName, clazz);
        return jmongo.find(collection, id);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var ref = (JmongoRef) o;
        if (!id.equals(ref.id)) {
            return false;
        }
        if (!collectionName.equals(ref.collectionName)) {
            return false;
        }
        return Objects.equals(databaseName, ref.databaseName);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + collectionName.hashCode();
        result = 31 * result + (databaseName != null ? databaseName.hashCode() : 0);
        return result;
    }
}
