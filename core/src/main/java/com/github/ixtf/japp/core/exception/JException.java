package com.github.ixtf.japp.core.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.Map;

import static com.github.ixtf.japp.core.Constant.MAPPER;

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

    public JsonNode toJsonNode() {
        return MAPPER.createObjectNode()
                .put("errorCode", errorCode);
    }

}
