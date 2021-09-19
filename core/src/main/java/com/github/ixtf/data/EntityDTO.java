package com.github.ixtf.data;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;

@Data
public class EntityDTO implements Serializable {
    @NotBlank
    private String id;

    public static EntityDTO from(String id) {
        Validate.notBlank(id);
        final var ret = new EntityDTO();
        ret.setId(id);
        return ret;
    }
}
