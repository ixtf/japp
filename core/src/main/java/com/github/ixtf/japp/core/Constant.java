package com.github.ixtf.japp.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        public static final String MULTI = "E00001";
        public static final String AUTHENTICATION = "E00002";
        //权限不足
        public static final String AUTHORIZATION = "E00003";
        public static final String TOKEN = "E00004";
    }
//    public static void main(String[] args) {
//        UUID uuid = UUID.nameUUIDFromBytes("SHARE_JWT_KEY_ORG_JZB".getBytes(StandardCharsets.UTF_8));
//        System.out.println(uuid);
//        uuid = UUID.nameUUIDFromBytes("PERMANENT_JWT_KEY_ORG_JZB".getBytes(StandardCharsets.UTF_8));
//        System.out.println(uuid);
//    }
}
