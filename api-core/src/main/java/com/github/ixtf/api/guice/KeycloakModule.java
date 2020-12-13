package com.github.ixtf.api.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.Function;
import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import lombok.Getter;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;

import java.util.function.Consumer;

import static com.github.ixtf.api.guice.ApiModule.CONFIG;
import static com.github.ixtf.guice.GuiceModule.getInstance;

public class KeycloakModule extends AbstractModule {

    @Provides
    private Keycloak Keycloak(@Named(CONFIG) JsonObject rootConfig) {
        final var config = rootConfig.getJsonObject("keycloak-admin");
        return Keycloak.getInstance(
                config.getString("serverUrl"),
                config.getString("master", "master"),
                config.getString("username"),
                config.getString("password"),
                config.getString("clientId", "admin-cli")
        );
    }

    @Singleton
    @Provides
    private KeycloakCall KeycloakCall(@Named(CONFIG) JsonObject rootConfig) {
        final var config = rootConfig.getJsonObject("keycloak-admin");
        return new KeycloakCall(config.getString("realm"));
    }

    public static class KeycloakCall {
        @Getter
        private final String realm;

        private KeycloakCall(String realm) {
            this.realm = realm;
        }

        public void run(Consumer<RealmResource> consumer) {
            @Cleanup final var keycloak = getInstance(Keycloak.class);
            final var realmResource = keycloak.realm(realm);
            consumer.accept(realmResource);
        }

        public <T> T call(Function<RealmResource, T> function) {
            @Cleanup final var keycloak = getInstance(Keycloak.class);
            final var realmResource = keycloak.realm(realm);
            return function.apply(realmResource);
        }
    }

}
