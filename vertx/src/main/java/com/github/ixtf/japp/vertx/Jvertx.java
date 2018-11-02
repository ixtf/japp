package com.github.ixtf.japp.vertx;

import com.github.ixtf.japp.core.Constant;
import com.github.ixtf.japp.core.J;
import com.github.ixtf.japp.core.exception.JException;
import com.github.ixtf.japp.core.exception.JMultiException;
import com.github.ixtf.japp.vertx.spi.ApiGateway;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.ixtf.japp.core.Constant.MAPPER;

/**
 * @author jzb 2018-10-27
 */
@Slf4j
public class Jvertx {
    private static ApiGateway apiGateway;

    public static final String getRouterPath(Path pathAnnotation) {
        if (pathAnnotation == null) {
            return "";
        }
        String path = pathAnnotation.value();
        if (J.isBlank(path)) {
            return "";
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 2);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return toVertxPath(path);
    }

    public static String toVertxPath(final String path) {
        String result = path;
        Pattern pattern = Pattern.compile("(\\{\\w+\\})", Pattern.DOTALL);
        final Matcher m = pattern.matcher(path);
        while (m.find()) {
            final int start = m.start();
            final int end = m.end();
            final String replace = path.substring(start, end);
            final String pathParam = ":" + replace.substring(1, replace.length() - 1);
            result = result.replace(replace, pathParam);
        }
        return result;
    }

    public static void enableCommon(Router router) {
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
    }

    public static void enableCommon(Router router, SessionStore sessionStore) {
        enableCommon(router);
        router.route().handler(SessionHandler.create(sessionStore));
    }

    public static void failureHandler(RoutingContext rc) {
        final HttpServerResponse response = rc.response().setStatusCode(400);
        final Throwable failure = rc.failure();
        final JsonObject result = new JsonObject();
        // todo response content-type 为 json
        if (failure instanceof JMultiException) {
            final JMultiException ex = (JMultiException) failure;
            final JsonArray errors = new JsonArray();
            result.put("errorCode", Constant.ErrorCode.MULTI)
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
            result.put("errorCode", Constant.ErrorCode.SYSTEM)
                    .put("errorMessage", failure.getLocalizedMessage());
        }
        response.end(result.encode());
    }

    public static <T> T readCommand(Class<T> clazz, String json) throws IOException, JException {
        final T command = MAPPER.readValue(json, clazz);
        return checkCommand(command);
    }

    public static <T> T checkCommand(T command) throws JException {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        final Validator validator = validatorFactory.getValidator();
        final Set<ConstraintViolation<T>> violations = validator.validate(command);
        if (violations.size() == 0) {
            return command;
        }
        final List<JException> exceptions = violations.stream()
                .map(violation -> {
                    final String propertyPath = violation.getPropertyPath().toString();
                    return new JException(Constant.ErrorCode.SYSTEM, propertyPath + ":" + violation.getMessage());
                })
                .collect(Collectors.toList());
        if (violations.size() == 1) {
            throw exceptions.get(0);
        }
        throw new JMultiException(exceptions);
    }

    public synchronized static ApiGateway apiGateway() {
        if (apiGateway == null) {
            final ServiceLoader<ApiGateway> load = ServiceLoader.load(ApiGateway.class);
            apiGateway = IterableUtils.get(load, 0);
        }
        return apiGateway;
    }

    public static <T> T getProxy(Class<T> clazz) {
        return apiGateway().getProxy(clazz);
    }
}
