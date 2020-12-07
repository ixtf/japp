package test;

import com.github.ixtf.persistence.mongo.Jmongo;
import com.github.ixtf.persistence.mongo.JmongoOptions;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.Document;
import reactor.core.publisher.Mono;
import test.domain.Operator;

public class DevMongo extends JmongoOptions {
    public static final Jmongo devMongo = Jmongo.of(DevMongo.class);
    public static final Mono<Operator> operator$ = devMongo.find(Operator.class, "85950916-1128-478e-b81a-6b7d72e3f249").cache();

    @Override
    protected MongoClient client() {
        final var connection_string = "mongodb://root:dev@dev.medipath.com.cn";
        final var builder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connection_string));
        return MongoClients.create(builder.build());
    }

    @Override
    public String dbName() {
        return "alg-check";
    }

    public static void main(String[] args) {
        final var block = operator$.block();
        System.out.println(block);
    }
}
