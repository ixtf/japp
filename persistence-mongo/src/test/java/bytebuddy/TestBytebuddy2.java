package bytebuddy;

import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class TestBytebuddy2 {
    @SneakyThrows
    public static void main(String[] args) {
        final var dynamicType = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class)).intercept(FixedValue.value("One!"))
                .method(named("foo")).intercept(FixedValue.value("Two!"))
                .method(named("foo").and(takesArguments(1))).intercept(FixedValue.value("Three!"))
                .make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();
        final var proxy = dynamicType.getDeclaredConstructor().newInstance();
        System.out.println(proxy.bar());
        System.out.println(proxy.foo());
        System.out.println(proxy.foo(null));
    }

    public static class Foo {
        public String bar() {
            return null;
        }

        public String foo() {
            return null;
        }

        public String foo(Object o) {
            return null;
        }
    }
}
