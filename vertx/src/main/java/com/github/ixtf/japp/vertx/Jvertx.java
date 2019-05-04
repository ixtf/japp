package com.github.ixtf.japp.vertx;

import com.github.ixtf.japp.core.Constant;
import com.github.ixtf.japp.core.J;
import com.github.ixtf.japp.core.exception.JError;
import com.github.ixtf.japp.vertx.spi.ApiGateway;
import com.google.common.collect.Sets;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.SessionStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;

import javax.validation.*;
import javax.ws.rs.Path;
import java.util.Collection;
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
        router.route().handler(BodyHandler.create());
        router.route().handler(ResponseContentTypeHandler.create());
        router.route().handler(CookieHandler.create());
    }

    public static void enableCommon(Router router, SessionStore sessionStore) {
        enableCommon(router);
        router.route().handler(SessionHandler.create(sessionStore));
    }

    public static void enableCors(Router router, Set<String> domains) {
        final Collection<String> patterns = Sets.newHashSet("localhost", "127\\.0\\.0\\.1");
        patterns.addAll(domains);
        final String domainP = patterns.stream().collect(Collectors.joining("|"));
        router.route().handler(CorsHandler.create("^http(s)?://(" + domainP + ")(:[1-9]\\d+)?")
                .allowCredentials(false)
                .allowedHeader("x-requested-with")
                .allowedHeader("access-control-allow-origin")
                .allowedHeader("origin")
                .allowedHeader("content-type")
                .allowedHeader("accept")
                .allowedHeader("authorization")
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.HEAD)
                .allowedMethod(HttpMethod.OPTIONS));
    }

    public static void failureHandler(RoutingContext rc) {
        final HttpServerResponse response = rc.response().setStatusCode(400);
        final Throwable failure = rc.failure();
        final JsonObject result = new JsonObject();
        // todo response content-type 为 json
        if (failure instanceof JError) {
            final JError ex = (JError) failure;
            result.put("errorCode", ex.getErrorCode())
                    .put("errorMessage", ex.getMessage());
        } else {
            result.put("errorCode", Constant.ErrorCode.SYSTEM)
                    .put("errorMessage", failure.getLocalizedMessage());
        }
        response.end(result.encode());
    }

    @SneakyThrows
    public static <T> T readCommand(Class<T> clazz, String json) {
        final T command = MAPPER.readValue(json, clazz);
        return checkCommand(command);
    }

    public static <T> T checkCommand(T command) {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        final Validator validator = validatorFactory.getValidator();
        final Set<ConstraintViolation<T>> violations = validator.validate(command);
        if (violations.size() == 0) {
            return command;
        }
        final List<Throwable> exceptions = violations.stream().map(violation -> {
            final String propertyPath = violation.getPropertyPath().toString();
            return new RuntimeException(propertyPath + ":" + violation.getMessage());
        }).collect(Collectors.toList());
        throw new ConstraintViolationException(violations);
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
