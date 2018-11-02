package com.github.ixtf.japp.vertx.annotations;

import java.lang.annotation.*;

/**
 * @author jzb 2018-10-27
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Address {
    /**
     * Target address
     *
     * @return String address value on EventBus
     */
    String value();
}
