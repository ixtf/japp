package com.github.ixtf.data;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class EntityDTO implements Serializable {
    @NotBlank
    private String id;
}
