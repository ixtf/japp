package com.github.ixtf.persistence.lucene;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jzb 2019-11-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LuceneCommandOne extends LuceneCommandAll {
    @NotBlank
    private String id;
}
