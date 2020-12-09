package bytebuddy;

import lombok.Getter;
import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bson.Document;
import reactor.core.publisher.Mono;
import test.domain.Operator;

import jakarta.persistence.Transient;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.github.ixtf.persistence.mongo.Jmongo.ID_COL;
import static com.mongodb.client.model.Filters.eq;
import static java.util.stream.Collectors.toUnmodifiableList;
import static pro.DevMongo.devMongo;

public class TestBytebuddy {
    public static final String MAP$JMONGO$ = "MAP$JMONGO$";
    @Getter(lazy = true)
    private final Operator test = testOperator();

    @SneakyThrows
    public static void main(String[] args) {
        testOperator();
    }

    private static Operator testOperator() {
        final var operator = find(Operator.class, "85950916-1128-478e-b81a-6b7d72e3f249").block();
        System.out.println(operator.toString());
        System.out.println(operator.getId());
        return operator;
    }

    private static <T> Mono<T> find(Class<T> clazz, String id) {
        final var block = new Document();
        IntStream.range(0, 20).parallel().forEach(it -> {
            block.append("test-" + it, it);
        });
        System.out.println(block);

        final var collection = devMongo.collection(clazz);
        final var map$ = Mono.from(collection.find(eq(ID_COL, id))).map(it -> Map.of("document", it));
        final var entity$ = Mono.fromCallable(() -> {
            final var dynamicType = dynamicType(clazz);
            return dynamicType.getDeclaredConstructor().newInstance();
        });
        return Mono.zip(map$, entity$).flatMap(tuple -> Mono.fromCallable(() -> {
            final var t = tuple.getT2();
            FieldUtils.writeField(t, MAP$JMONGO$, tuple.getT1());
            return t;
        }));
    }

    private static <T> Class<? extends T> dynamicType(Class<T> clazz) {
        final var fields = Arrays.stream(FieldUtils.getAllFields(clazz))
                .parallel()
                .filter(it -> !Modifier.isStatic(it.getModifiers()))
                .filter(it -> Objects.nonNull(it.getAnnotation(Transient.class)))
                .collect(toUnmodifiableList());
        return new ByteBuddy()
                .subclass(clazz)
                .defineField(MAP$JMONGO$, Map.class, Visibility.PUBLIC)
                .make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();
    }
}
