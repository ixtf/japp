package com.github.ixtf.japp.vertx.annotations;

import lombok.Data;

import java.io.Serializable;
import java.lang.annotation.*;

@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FileDownload {
    @Data
    class DTO implements Serializable {
        private String fileName;
        private byte[] content;
    }
}