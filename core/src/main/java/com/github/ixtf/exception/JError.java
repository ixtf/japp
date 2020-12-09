package com.github.ixtf.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.Map;

import static com.github.ixtf.Constant.MAPPER;

public class JError extends Error {
    @Getter
    private final String errorCode;
    @Getter
    private final Map<String, String> params;

    public JError(String errorCode) {
        this.errorCode = errorCode;
        this.params = null;
    }

    public JError(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.params = null;
    }

    public JError(String errorCode, Map<String, String> params) {
        this.errorCode = errorCode;
        this.params = params;
    }

    public JsonNode toJsonNode() {
        return MAPPER.createObjectNode()
                .put("errorCode", errorCode);
    }

}
