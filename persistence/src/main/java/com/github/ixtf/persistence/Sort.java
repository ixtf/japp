package com.github.ixtf.persistence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class Sort implements Serializable {
    @NotBlank
    private String id;
    @NotNull
    private SortStart start;
}
