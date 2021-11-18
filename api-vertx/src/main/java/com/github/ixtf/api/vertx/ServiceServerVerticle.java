package com.github.ixtf.api.vertx;

import com.github.ixtf.J;
import com.github.ixtf.api.ApiAction;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Collection;

import static com.github.ixtf.api.guice.ApiModule.ACTIONS;
import static com.github.ixtf.api.guice.ApiModule.SERVICE;
import static com.github.ixtf.guice.GuiceModule.injectMembers;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
public class ServiceServerVerticle extends AbstractVerticle {
  @Named(SERVICE)
  @Inject
  private String service;

  @Named(ACTIONS)
  @Inject
  private Collection<Method> methods;

  @Override
  public void start(Promise<Void> startPromise) {
    injectMembers(this);
    methods.stream()
        .map(this::consumer)
        .collect(collectingAndThen(toUnmodifiableList(), CompositeFuture::all))
        .<Void>mapEmpty()
        .onComplete(startPromise);
  }

  private Future consumer(Method method) {
    return Future.<Void>future(
        p -> {
          final var annotation = method.getAnnotation(ApiAction.class);
          final var service =
              ofNullable(annotation.service()).filter(J::nonBlank).orElse(this.service);
          final var action = annotation.action();
          final var address = String.join(":", service, action);
          final var handler = injectMembers(new ApiReplyHandler(address, method));
          final var consumer = vertx.eventBus().consumer(address).handler(handler);
          consumer.completionHandler(p);
        });
  }
}
