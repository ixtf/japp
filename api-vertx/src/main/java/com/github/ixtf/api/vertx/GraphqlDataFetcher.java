package com.github.ixtf.api.vertx;

import com.github.ixtf.api.GraphqlAction;
import com.github.ixtf.api.Util;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.ext.web.handler.graphql.schema.VertxDataFetcher;
import jakarta.ws.rs.QueryParam;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.ixtf.guice.GuiceModule.getInstance;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableSet;

public class GraphqlDataFetcher implements DataFetcher<Object> {
    private final Object instance;
    private final Method method;
    private final Function<DataFetchingEnvironment, Object>[] paramFuns;
    private final Function<Object, Object> retFun;

    private GraphqlDataFetcher(Method method) {
        this.method = method;
        paramFuns = paramFuns(method);
        retFun = retFun(method);
        instance = getInstance(method.getDeclaringClass());
    }

    public static Collection<Pair<GraphqlAction, ? extends DataFetcher>> generate(Stream<Class<?>> streamClass, Stream<Method> streamMethod) {
        final var ret = Stream.concat(
                streamClass.map(clazz -> {
                    final var annotation = clazz.getAnnotation(GraphqlAction.class);
                    final var instance = getInstance(clazz);
                    if (instance instanceof final DataFetcher dataFetcher) {
                        return Pair.of(annotation, dataFetcher);
                    } else if (instance instanceof final BiConsumer biConsumer) {
                        return Pair.of(annotation, VertxDataFetcher.create(biConsumer));
                    } else if (instance instanceof final Function function) {
                        return Pair.of(annotation, VertxDataFetcher.create(function));
                    }
                    throw new RuntimeException();
                }),
                streamMethod.map(method -> {
                    final var annotation = method.getAnnotation(GraphqlAction.class);
                    return Pair.of(annotation, new GraphqlDataFetcher(method));
                })
        ).collect(toUnmodifiableSet());
        ret.parallelStream().collect(groupingBy(pair -> {
            final var annotation = pair.getKey();
            final var type = annotation.type();
            final var action = annotation.action();
            return String.join(":", type.name(), action);
        })).forEach((k, v) -> {
            if (v.size() > 1) {
                throw new RuntimeException("graphql地址重复 [" + k + "]");
            }
        });
        return ret;
    }

    private Function<DataFetchingEnvironment, Object>[] paramFuns(Method method) {
        final var ret = new Function[method.getParameterCount()];
        if (ret.length > 0) {
            final var parameters = method.getParameters();
            for (var i = 0; i < ret.length; i++) {
                ret[i] = paramFun(parameters[i]);
            }
        }
        return ret;
    }

    private Function<DataFetchingEnvironment, Object> paramFun(Parameter parameter) {
        final var type = parameter.getType();
        if (DataFetchingEnvironment.class.isAssignableFrom(type)) {
            return (Function) Function.identity();
        }
        if (Principal.class.isAssignableFrom(type)) {
            return Util::principal;
        }
        final var queryParam = parameter.getAnnotation(QueryParam.class);
        if (queryParam != null) {
            final var value = queryParam.value();
            return env -> env.getArgument(value);
        }
        if (String.class.isAssignableFrom(type)) {
            return env -> env.getArgument("command");
        }
        return env -> Util.checkAndGetCommand(env, type);
    }

    private Function<Object, Object> retFun(Method method) {
        final var type = method.getReturnType();
        if (Mono.class.isAssignableFrom(type)) {
            return o -> ((Mono) o).toFuture();
        }
        if (Flux.class.isAssignableFrom(type)) {
            return o -> ((Flux) o).collectList().toFuture();
        }
        return Function.identity();
    }

    @Override
    public Object get(DataFetchingEnvironment env) throws Exception {
        final var objects = Arrays.stream(paramFuns).map(it -> it.apply(env)).toArray();
        final var invoke = method.invoke(instance, objects);
        return retFun.apply(invoke);
    }
}
