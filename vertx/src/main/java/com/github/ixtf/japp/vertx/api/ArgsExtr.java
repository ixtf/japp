package com.github.ixtf.japp.vertx.api;

import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.github.ixtf.japp.core.J;
import com.github.ixtf.japp.core.exception.JException;
import com.github.ixtf.japp.vertx.Jvertx;
import com.google.common.collect.Sets;
import com.sun.security.auth.UserPrincipal;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.RoutingContext;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.github.ixtf.japp.core.Constant.MAPPER;

/**
 * @author jzb 2018-11-01
 */
public class ArgsExtr {
    private final Parameter[] parameters;
    @Getter
    private final boolean hasPrincipal;

    public ArgsExtr(Method method) {
        parameters = method.getParameters();
        hasPrincipal = ArrayUtils.isEmpty(parameters) ? false : Arrays.stream(parameters)
                .map(Parameter::getType)
                .filter(Principal.class::isAssignableFrom)
                .findFirst()
                .isPresent();
    }

    public JsonArray extr(RoutingContext rc, String principal) throws IOException, JException {
        final JsonArray result = new JsonArray();
        if (ArrayUtils.isEmpty(parameters)) {
            return new JsonArray();
        }
        for (Parameter parameter : parameters) {
            result.add(getValue(parameter, rc, principal));
        }
        return result;
    }

    private Object getValue(Parameter parameter, RoutingContext rc, String principal) throws IOException, JException {
        final Class<?> type = parameter.getType();
        if (Principal.class.isAssignableFrom(type)) {
            return principal;
        }

        final PathParam pathParam = parameter.getAnnotation(PathParam.class);
        if (pathParam != null) {
            final String name = pathParam.value();
            return rc.pathParam(name);
        }

        final QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
        if (queryParam != null) {
            final String name = queryParam.value();
            final List<String> values = ListUtils.emptyIfNull(rc.queryParam(name));

            if (List.class.isAssignableFrom(type)) {
                return values;
            } else if (Set.class.isAssignableFrom(type)) {
                return Sets.newHashSet(values);
            }

            final String value = J.isEmpty(values) ? null : values.get(0);
            if (value != null) {
                return value;
            }

            final DefaultValue defaultValue = parameter.getAnnotation(DefaultValue.class);
            if (defaultValue != null) {
                return defaultValue.value();
            }
            return J.defaultString(value);
        }

        if (Collection.class.isAssignableFrom(type)) {
            return rc.getBodyAsJsonArray();
        }

        final String bodyAsString = rc.getBodyAsString();
        if (J.isBlank(bodyAsString)) {
            return null;
        }

        final Object command = Jvertx.readCommand(type, bodyAsString);
        return JsonObject.mapFrom(command);
    }

    public Object[] extr(Message<JsonArray> reply) throws IOException {
        final JsonArray args = reply.body();
        if (args == null || args.isEmpty()) {
            return new Object[0];
        }

        final Object[] result = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            final Object value = convert(parameters[i], args.getValue(i));
            result[i] = value;
        }
        return result;
    }

    private Object convert(Parameter parameter, Object arg) throws IOException {
        if (arg == null) {
            return null;
        }
        final Class<?> type = parameter.getType();
        if (Principal.class.isAssignableFrom(type)) {
            final Principal principal = new UserPrincipal((String) arg);
            return principal;
        }
        if (String.class.isAssignableFrom(type)) {
            return arg;
        }
        if (Long.class.isAssignableFrom(type)) {
            return new Long(arg.toString());
        }
        if (long.class.isAssignableFrom(type)) {
            return new Long(arg.toString()).longValue();
        }
        if (Integer.class.isAssignableFrom(type)) {
            return new Integer(arg.toString());
        }
        if (int.class.isAssignableFrom(type)) {
            return new Integer(arg.toString()).intValue();
        }
        if (Collection.class.isAssignableFrom(type)) {
            final ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
            final Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
            final CollectionLikeType collectionLikeType = MAPPER.getTypeFactory().constructCollectionLikeType(type, actualTypeArgument.getClass());
            return MAPPER.readValue(((JsonArray) arg).encode(), collectionLikeType);
        }
        return MAPPER.readValue(((JsonObject) arg).encode(), type);
    }

    public Single<JsonObject> toMessage(RoutingContext rc) {
        return null;
    }
}
