package com.github.ixtf.persistence.lucene;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotBlank;

/**
 * @author jzb 2019-11-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LuceneCommandOne extends LuceneCommandAll {
    @NotBlank
    private String id;
}
