package com.github.ixtf.api.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.Function;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class KeycloakModule extends AbstractModule {
    private static final String ADDRESS = "__com.github.ixtf.api:KeycloakAdmin__";

    @Singleton
    @Provides
    private KeycloakRealm KeycloakRealm(Vertx vertx) {
        return new KeycloakRealm(Mono.defer(() -> {
            final var config = vertx.eventBus().<JsonObject>request(ADDRESS, null).map(Message::body);
            return Mono.fromCompletionStage(config.toCompletionStage());
        }));
    }

    private Keycloak keycloak(JsonObject config) {
        return Keycloak.getInstance(
                config.getString("serverUrl"),
                config.getString("master", "master"),
                config.getString("username"),
                config.getString("password"),
                config.getString("clientId", "admin-cli")
        );
    }

    private RealmResource realmResource(Keycloak keycloak, JsonObject config) {
        return keycloak.realm(config.getString("realm"));
    }

    public class KeycloakRealm {
        private final Mono<JsonObject> config$;

        private KeycloakRealm(Mono<JsonObject> config$) {
            this.config$ = config$.cache();
        }

        public Mono<Void> run(Consumer<RealmResource> consumer) {
            return config$.doOnSuccess(config -> {
                @Cleanup final var keycloak = keycloak(config);
                consumer.accept(realmResource(keycloak, config));
            }).then();
        }

        public <T> Mono<T> call(Function<RealmResource, T> function) {
            return config$.map(config -> {
                @Cleanup final var keycloak = keycloak(config);
                return function.apply(realmResource(keycloak, config));
            });
        }
    }
}

