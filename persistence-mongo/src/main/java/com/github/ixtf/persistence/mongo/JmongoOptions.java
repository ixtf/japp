package com.github.ixtf.persistence.mongo;

import com.mongodb.reactivestreams.client.MongoClient;

/**
 * @author jzb 2019-02-18
 */
public abstract class JmongoOptions {

    protected abstract MongoClient client();

    protected String dbName() {
        return "test-db";
    }

    protected EntityCacheOptions entityCacheOptions() {
        return EntityCacheOptions.builder().build();
    }

}
