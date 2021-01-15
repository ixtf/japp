package com.github.ixtf.persistence.lucene;

import com.github.ixtf.persistence.IEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;

/**
 * @author jzb 2019-11-12
 */
@Data
public class LuceneCommandAll implements Serializable {
    @NotBlank
    private String className;

    @SneakyThrows(ClassNotFoundException.class)
    public Class<? extends IEntity> getClazz() {
        return (Class<? extends IEntity>) Class.forName(className);
    }
}
