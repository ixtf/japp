package com.github.ixtf.persistence;

import java.io.Serializable;

public interface IOperator extends Serializable {
    String getId();

    void setId(String id);

    String getName();

    void setName(String name);
}
