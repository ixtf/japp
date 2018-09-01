package com.github.ixtf.japp.vertx;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ixtf.japp.core.exception.JException;
import com.github.ixtf.japp.core.exception.JMultiException;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.github.ixtf.japp.core.Constant.ErrorCode;
import static com.github.ixtf.japp.core.Constant.MAPPER;

/**
 * @author jzb 2018-08-17
 */
public interface Jvertx {
    static void enableCommon(Router router, SessionStore sessionStore) {
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");
        allowHeaders.add("Authorization");
        router.route().handler(CorsHandler.create("*")
                .allowedHeaders(allowHeaders)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PATCH));
        router.route().handler(BodyHandler.create());
        router.route().handler(ResponseContentTypeHandler.create());
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(sessionStore));
    }

    static void failureHandler(RoutingContext rc) {
        final HttpServerResponse response = rc.response().setStatusCode(400);
        final Throwable failure = rc.failure();
        final JsonObject result = new JsonObject();
        // todo response content-type 为 json
        if (failure instanceof JMultiException) {
            final JMultiException ex = (JMultiException) failure;
            final JsonArray errors = new JsonArray();
            result.put("errorCode", ErrorCode.MULTI)
                    .put("errors", errors);
            ex.getExceptions().forEach(it -> {
                final JsonObject error = new JsonObject()
                        .put("errorCode", it.getErrorCode())
                        .put("errorMessage", it.getMessage());
                errors.add(error);
            });
        } else if (failure instanceof JException) {
            final JException ex = (JException) failure;
            result.put("errorCode", ex.getErrorCode())
                    .put("errorMessage", ex.getMessage());
        } else {
            result.put("errorCode", ErrorCode.SYSTEM)
                    .put("errorMessage", failure.getLocalizedMessage());
        }
        response.end(result.encode());
    }

    static <T> T readCommand(Class<T> clazz, RoutingContext rc) throws Exception {
        return readCommand(clazz, rc.getBodyAsString());
    }

    static <T> T readCommand(Class<T> clazz, JsonObject jsonObject) throws Exception {
        final T command = jsonObject.mapTo(clazz);
        return checkCommand(command);
    }

    static <T> T readCommand(Class<T> clazz, JsonNode node) throws Exception {
        final T command = MAPPER.convertValue(node, clazz);
        return checkCommand(command);
    }

    static <T> T readCommand(Class<T> clazz, String json) throws Exception {
        final T command = MAPPER.readValue(json, clazz);
        return checkCommand(command);
    }

    static <T> T checkCommand(T command) throws JException {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        final Validator validator = validatorFactory.getValidator();
        final Set<ConstraintViolation<T>> violations = validator.validate(command);
        if (violations.size() == 0) {
            return command;
        }
        final List<JException> exceptions = violations.stream()
                .map(violation -> {
                    final String propertyPath = violation.getPropertyPath().toString();
                    return new JException(ErrorCode.SYSTEM, propertyPath + ":" + violation.getMessage());
                })
                .collect(Collectors.toList());
        if (violations.size() == 1) {
            throw exceptions.get(0);
        }
        throw new JMultiException(exceptions);
    }

    static <T> SingleObserver<T> toSingleObserver(Message reply) {
        AtomicBoolean completed = new AtomicBoolean();
        return new SingleObserver<T>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull T item) {
                if (completed.compareAndSet(false, true)) {
                    reply.reply(item);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (completed.compareAndSet(false, true)) {
                    handleError(reply, error);
                }
            }
        };
    }

    static CompletableObserver toCompletableObserver(Message reply) {
        AtomicBoolean completed = new AtomicBoolean();
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onComplete() {
                if (completed.compareAndSet(false, true)) {
                    reply.reply(null);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (completed.compareAndSet(false, true)) {
                    handleError(reply, error);
                }
            }
        };
    }

    static void handleError(Message reply, Throwable error) {
        LoggerFactory.getLogger(reply.address()).error("", error);
        reply.fail(-1, error.getMessage());
    }

    interface EB {
        static void noAction(Message reply) {
            reply.fail(-1, "action not exist");
        }

        interface Header {
            String action = "action";
        }

        interface Body {
            String principal = "principal";
        }
    }
}
