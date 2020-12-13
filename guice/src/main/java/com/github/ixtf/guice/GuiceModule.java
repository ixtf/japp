package com.github.ixtf.guice;

import com.google.inject.Module;
import com.google.inject.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GuiceModule extends AbstractModule {
    private static Injector INJECTOR;

    synchronized public static void init(Module... modules) {
        if (INJECTOR == null) {
            INJECTOR = Guice.createInjector(modules);
        }
    }

    public static <T> T getInstance(Class<T> type) {
        return INJECTOR.getInstance(type);
    }

    public static <T> T getInstance(Class<T> type, Annotation annotation) {
        return INJECTOR.getInstance(Key.get(type, annotation));
    }

    public static <T> T injectMembers(T o) {
        INJECTOR.injectMembers(o);
        return o;
    }

}
