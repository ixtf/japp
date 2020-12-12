package test;

import com.github.ixtf.api.guice.MongoModule;
import com.github.ixtf.api.guice.TracerModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import io.opentracing.Tracer;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static com.github.ixtf.api.guice.ApiModule.CONFIG;

public class TestGuice extends AbstractModule {
    @Inject
    private Optional<Tracer> tracerOpt;

    @SneakyThrows
    public static void main(String[] args) {
        for (Constructor<?> constructor : MongoModule.Options.class.getConstructors()) {
            System.out.println(constructor.newInstance());
        }


        final var testModule = new TestGuice();
        final var tracerModule = new TracerModule("test");
        final var injector = Guice.createInjector(testModule, tracerModule);

        injector.injectMembers(testModule);
        System.out.println(testModule.tracerOpt);
    }

    @Override
    protected void configure() {
        bind(JsonObject.class).annotatedWith(Names.named(CONFIG)).toInstance(new JsonObject());
    }
}
