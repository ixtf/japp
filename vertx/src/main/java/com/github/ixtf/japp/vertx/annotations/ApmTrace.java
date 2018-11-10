package com.github.ixtf.japp.vertx.annotations;

import java.lang.annotation.*;

/**
 * @author jzb 2018-10-27
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApmTrace {
    String type();
}
