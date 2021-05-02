package com.github.ixtf.api.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.json.JsonObject;

import static com.github.ixtf.api.guice.ApiModule.CONFIG;

public class GraphqlModule extends AbstractModule {
    public static final String DIR = "com.github.ixtf.persistence.mongo.guice.LuceneModule:__DIR__";

    @Named(DIR)
    @Singleton
    @Provides
    private String Lucene_DIR(@Named(CONFIG) JsonObject rootConfig) {
        final var config = rootConfig.getJsonObject("lucene");
        return config.getString("dir");
    }

}
