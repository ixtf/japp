package com.github.ixtf.japp.core.exception;

import lombok.Getter;

import java.util.Map;

public class JException extends Exception {
    @Getter
    private final String errorCode;
    @Getter
    private final Map<String, String> params;

    public JException(String errorCode) {
        this.errorCode = errorCode;
        this.params = null;
    }

    public JException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.params = null;
    }

    public JException(String errorCode, Map<String, String> params) {
        this.errorCode = errorCode;
        this.params = params;
    }

}
