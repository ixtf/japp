package com.github.ixtf.api;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphqlAction {
    String service();

    String action();
}
