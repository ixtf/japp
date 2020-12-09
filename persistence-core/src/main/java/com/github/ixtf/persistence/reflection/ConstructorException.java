package com.github.ixtf.persistence.reflection;

public class ConstructorException extends RuntimeException {

    public ConstructorException(Class clazz) {
        super("需要不带参数的构造函数: " + clazz.getName());
    }
}