package com.github.ixtf.mongo;

import com.github.ixtf.J;
import org.apache.commons.lang3.Validate;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class JmongoRefCodec implements Codec<JmongoRef> {
    private static final String ID_COL = "$id";
    private static final String COLLECTION_NAME_COL = "$ref";
    private static final String DATABASE_NAME_COL = "$db";
    private final CodecRegistry registry;

    JmongoRefCodec(final CodecRegistry registry) {
        this.registry = Validate.notNull(registry, "registry");
    }

    @Override
    public void encode(BsonWriter writer, JmongoRef value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        // $id field without a $ref before it, which is invalid.
        writer.writeString(ID_COL, value.getId());
        writer.writeString(COLLECTION_NAME_COL, value.getCollectionName());
        ofNullable(value.getDatabaseName()).filter(J::nonBlank).ifPresent(it -> writer.writeString(DATABASE_NAME_COL, it));
        writer.writeEndDocument();
    }

    @Override
    public JmongoRef decode(BsonReader reader, DecoderContext decoderContext) {
        String id = null;
        String collectionName = null;
        String databaseName = null;

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            final var name = reader.readName();
            final var value = reader.readString();
            switch (name) {
                case ID_COL: {
                    id = value;
                    break;
                }
                case COLLECTION_NAME_COL: {
                    collectionName = value;
                    break;
                }
                case DATABASE_NAME_COL: {
                    databaseName = value;
                    break;
                }
                default: {
                    throw new CodecConfigurationException(format("解码出错： '%s' for '%s'", JmongoRef.class.getClass().getName(), name));
                }
            }
        }
        reader.readEndDocument();

        if (J.isBlank(id)) {
            return null;
        }
        return new JmongoRef(databaseName, collectionName, id);
    }

    @Override
    public Class<JmongoRef> getEncoderClass() {
        return JmongoRef.class;
    }
}
