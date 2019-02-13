package com.github.ixtf.japp.vertx.spi.internal;

import com.github.ixtf.japp.vertx.api.ApiRoute;
import com.github.ixtf.japp.vertx.api.ClassResource;
import com.github.ixtf.japp.vertx.api.MethodResource;
import com.github.ixtf.japp.vertx.spi.ApiGateway;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * @author jzb 2018-11-01
 */
@Slf4j
public abstract class AbstractApiGateway implements ApiGateway {
    protected ClassLoader classLoader;
    protected ClassPath classPath;

    @Override
    public Completable rxMount(Router router) {
        return rxListApiRoutes().flatMapCompletable(apiRoute -> apiRoute.rxMount(router));
    }

    @Override
    public Completable rxConsume(Vertx vertx) {
        return rxListApiRoutes().flatMapCompletable(apiRoute -> apiRoute.rxConsume(vertx));
    }

    protected abstract Flowable<String> rxListPackage();

    protected Flowable<ApiRoute> rxListApiRoutes() {
        return rxListPackage()
                .parallel()
                .map(classPath()::getTopLevelClassesRecursive)
                .flatMap(Flowable::fromIterable)
                .filter(defaultFilter())
                .map(ClassPath.ClassInfo::load)
                .filter(it -> Objects.nonNull(it.getAnnotation(Path.class)))
                .map(ClassResource::new)
                .sequential().toList()
                .flatMapPublisher(this::ensure)
                .map(ClassResource::getMethodResources)
                .flatMap(Flowable::fromIterable)
                .map(MethodResource::getApiRoute);
    }

    @SneakyThrows
    protected synchronized ClassPath classPath() {
        if (classPath == null) {
            classPath = ClassPath.from(classLoader());
        }
        return classPath;
    }

    protected synchronized ClassLoader classLoader() {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    private Predicate<ClassPath.ClassInfo> defaultFilter() {
        final Collection<String> excludePrefixes = ImmutableSet.of("java.", "javax.", "com.sun.");
        return classInfo -> {
            final String packageName = classInfo.getPackageName();
            for (String prefix : excludePrefixes) {
                if (StringUtils.startsWith(packageName, prefix)) {
                    return false;
                }
            }
            return true;
        };
    }

    private Flowable<ClassResource> ensure(Collection<ClassResource> classResources) {
        log.debug("all resources: " + classResources.size());
        // todo 增加子路由
        final Collection<ClassResource> result = Sets.newHashSet(classResources);
        log.debug("ensure resources: " + result.size());
        return Flowable.fromIterable(result);
    }
}
