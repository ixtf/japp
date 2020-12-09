package bytebuddy;

import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import test.domain.Operator;

public class TestBytebuddy1 {
    @SneakyThrows
    public static void main(String[] args) {
        final var dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .method(ElementMatchers.named("toString"))
                .intercept(FixedValue.value("Hello World!"))
                .make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();
        final var proxy = dynamicType.getDeclaredConstructor().newInstance();
        System.out.println(proxy.toString());
    }
}
