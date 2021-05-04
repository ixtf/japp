package test;

import com.github.ixtf.persistence.mongo.Jmongo;
import com.github.ixtf.persistence.mongo.JmongoOptions;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import test.domain.AlgConfig;
import test.domain.Operator;

public class DevMongo extends JmongoOptions {
    public static final Jmongo devMongo = Jmongo.of(DevMongo.class);

    public static <T> MongoCollection<T> collection(Class<T> clazz) {
        return devMongo.database().getCollection("T_" + clazz.getSimpleName(), clazz);
    }

    public static void main(String[] args) {
        System.out.println(Operator.clazz());
//        final var T_Operator = collection(Operator.class);
//        final var T_AlgConfig = collection(AlgConfig.class);

    }

    @Override
    protected MongoClient client() {
        final var pojoCodecProvider = PojoCodecProvider.builder()
                .register(Operator.class, AlgConfig.class)
                .build();
        final var defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        final var pojoCodecRegistry = CodecRegistries.fromProviders(pojoCodecProvider);
        final var codecRegistry = CodecRegistries.fromRegistries(defaultCodecRegistry, pojoCodecRegistry);

        final var connection_string = "mongodb://root:dev@dev.medipath.com.cn";
        final var builder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connection_string))
                .codecRegistry(codecRegistry);
        return MongoClients.create(builder.build());
    }

    @Override
    public String dbName() {
        return "test";
    }

    private BsonDocument getBsonDocument(MongoCollection collection, Object entity) {
        final var document = new BsonDocument();
        final Codec codec = collection.getCodecRegistry().get(entity.getClass());
        codec.encode(new BsonDocumentWriter(document), entity, EncoderContext.builder().build());
        return document;
    }
}
