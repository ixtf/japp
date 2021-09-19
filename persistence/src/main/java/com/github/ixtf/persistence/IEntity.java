package com.github.ixtf.persistence;

import java.io.Serializable;

/**
 * @author jzb 2019-02-28
 */
public interface IEntity extends Serializable {

    String getId();

    void setId(String id);

    boolean isDeleted();

    void setDeleted(boolean deleted);
}
