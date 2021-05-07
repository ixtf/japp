package com.github.ixtf.persistence.runtime;

import com.github.ixtf.persistence.IEntity;

import java.lang.annotation.Annotation;

/**
 * @author jzb 2019-04-30
 */
public interface PersistenceCallback {
    Class<? extends IEntity> getEntityClass();

    Class<? extends Annotation> getAnnotationClass();

    void callback(IEntity o);
}
