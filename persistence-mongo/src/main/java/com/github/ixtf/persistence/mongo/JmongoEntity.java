package com.github.ixtf.persistence.mongo;

import java.lang.invoke.MethodHandles;

public abstract class JmongoEntity {
    private static Class<?> clazz() {
        return MethodHandles.lookup().lookupClass();
    }

    public static <T extends JmongoEntity> T findById(Object id) {

    }

    public void persist() {
    }

    public void update() {
    }

    public void persistOrUpdate() {
    }
}
