package com.github.ixtf.api.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.Function;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class KeycloakModule extends AbstractModule {
    private static final String ADDRESS = "__com.github.ixtf.api:KeycloakAdmin__";
    @Getter
    private final String realm;

    public KeycloakModule(String realm) {
        this.realm = realm;
    }

    @Singleton
    @Provides
    private KeycloakRealm KeycloakRealm(Vertx vertx) {
        final var keycloakRealm = new KeycloakRealm(vertx);
        return keycloakRealm;
    }

    @NotNull
    private Keycloak keycloak(JsonObject config) {
        return Keycloak.getInstance(
                config.getString("serverUrl"),
                config.getString("master", "master"),
                config.getString("username"),
                config.getString("password"),
                config.getString("clientId", "admin-cli")
        );
    }

    public class KeycloakRealm {
        private final Vertx vertx;
        private final Mono<JsonObject> config$;

        private KeycloakRealm(Vertx vertx) {
            this.vertx = vertx;
            this.config$ = Mono.just(vertx.eventBus().<JsonObject>request(ADDRESS, null))
                    .map(Future::toCompletionStage)
                    .flatMap(Mono::fromCompletionStage)
                    .map(Message::body)
                    .cache();
        }

        public Mono<Void> run(Consumer<RealmResource> consumer) {
            return config$.doOnSuccess(config -> {
                @Cleanup final var keycloak = keycloak(config);
                final var realmResource = keycloak.realm(realm);
                consumer.accept(realmResource);
            }).then();
        }

        public <T> Mono<T> call(Function<RealmResource, T> function) {
            return config$.map(config -> {
                @Cleanup final var keycloak = keycloak(config);
                final var realmResource = keycloak.realm(realm);
                return function.apply(realmResource);
            });
        }
    }
}

