package bytebuddy;

import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;

public class TestBytebuddy4 {
    @SneakyThrows
    public static void main(String[] args) {
        final var dynamicUserType = new ByteBuddy()
                .subclass(UserType.class)
                .method(not(isDeclaredBy(Object.class)))
                .intercept(MethodDelegation.toField("interceptor"))
                .defineField("interceptor", Interceptor.class, Visibility.PRIVATE)
                .implement(InterceptionAccessor.class).intercept(FieldAccessor.ofBeanProperty())
                .make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();

        final var factory = new ByteBuddy()
                .subclass(InstanceCreator.class)
                .method(not(isDeclaredBy(Object.class)))
                .intercept(MethodDelegation.toConstructor(dynamicUserType))
                .make()
                .load(dynamicUserType.getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();

        final var userType = (UserType) factory.makeInstance();
        ((InterceptionAccessor) userType).setInterceptor(new HelloWorldInterceptor());
        userType.doSomething();
    }

    public interface Interceptor {
        String doSomethingElse();
    }

    public interface InterceptionAccessor {
        Interceptor getInterceptor();

        void setInterceptor(Interceptor interceptor);
    }

    public interface InstanceCreator {
        Object makeInstance();
    }

    public static class HelloWorldInterceptor implements Interceptor {
        @Override
        public String doSomethingElse() {
            return "Hello World!";
        }
    }

    public static class UserType {
        public String doSomething() {
            return null;
        }
    }
}
