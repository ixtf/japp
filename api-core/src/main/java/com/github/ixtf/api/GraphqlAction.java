package com.github.ixtf.api;

import java.lang.annotation.*;

import static com.github.ixtf.api.GraphqlAction.Type.QUERY;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphqlAction {
    Type type() default QUERY;

    String action();

    enum Type {
        QUERY,
        MUTATION,
    }
}
