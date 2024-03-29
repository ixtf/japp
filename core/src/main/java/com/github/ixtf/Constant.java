package com.github.ixtf;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Created by jzb on 15-12-5.
 */
public class Constant {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    public static final ObjectMapper YAML_MAPPER = new YAMLMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

//    public static final Pattern ZH_CN_YPP = Pattern.compile("([\u4e00-\u9fa5]+)");

    /**
     * jwt token 永久的加密key
     * UUID.nameUUIDFromBytes("SHARE_JWT_KEY_ORG_JZB".getBytes(StandardCharsets.UTF_8));
     */
    public static final String SHARE_JWT_KEY = "481c682c-c231-33ae-b1ec-1896f4c5021f";
    /**
     * jwt token 永久的加密key
     * UUID.nameUUIDFromBytes("PERMANENT_JWT_KEY_ORG_JZB".getBytes(StandardCharsets.UTF_8));
     */
    public static final String PERMANENT_JWT_KEY = "e664c8ed-9a57-353c-a0b0-e85d6e16a321";


    public static class ErrorCode {
        public static final String SYSTEM = "E00000";
        // 多错误类型
        public static final String MULTI = "E00000_0";
        public static final String AUTHENTICATION = "E00000_1";
        //权限不足
        public static final String AUTHORIZATION = "E00000_2";
        public static final String TOKEN = "E00000_3";
        public static final String EB_ACTION = "E00000_4";
    }

}
