package com.github.ixtf.mongo;

import com.github.ixtf.persistence.IOperator;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

public abstract class MongoEntityLoggable extends MongoEntityBase {
    @Getter
    @Setter
    private Operator creator;
    @Getter
    @Setter
    private Date createDateTime;
    @Getter
    @Setter
    private Operator modifier;
    @Getter
    @Setter
    private Date modifyDateTime;

    public void log(IOperator operator) {
        log(operator, new Date());
    }

    public void log(IOperator operator, Date date) {
        log(Operator.from(operator), date);
    }

    public void log(Operator operator) {
        log(operator, new Date());
    }

    public void log(Operator operator, Date date) {
        setModifier(operator);
        setModifyDateTime(date);
        if (getCreator() == null) {
            setCreator(operator);
            setCreateDateTime(date);
        }
    }

    @Data
    public static class Operator implements Serializable {
        private String id;
        private String name;

        public static Operator from(IOperator o) {
            final var operator = new Operator();
            operator.setId(o.getId());
            operator.setName(o.getName());
            return operator;
        }
    }

}
