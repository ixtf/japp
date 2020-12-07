package com.github.ixtf.persistence.mongo;

import lombok.Builder;
import lombok.Data;

/**
 * @author jzb 2019-11-15
 */
@Data
@Builder
public class EntityCacheOptions {
    @Builder.Default
    private final boolean cacheable = true;
    @Builder.Default
    private final long maximumSize = 10000;
}
