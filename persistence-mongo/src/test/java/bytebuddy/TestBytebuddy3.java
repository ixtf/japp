package bytebuddy;

import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class TestBytebuddy3 {
    @SneakyThrows
    public static void main(String[] args) {
        final var dynamicType = new ByteBuddy()
                .subclass(Source.class)
                .method(named("hello")).intercept(MethodDelegation.to(Target.class))
                .make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();
        final var proxy = dynamicType.getDeclaredConstructor().newInstance();
        System.out.println(proxy.hello("World"));
    }

    public static class Source {
        public String hello(String name) {
            return null;
        }
    }

    public static class Target {
        public static String hello(String name) {
            return "Hello " + name + "!";
        }
    }
}
