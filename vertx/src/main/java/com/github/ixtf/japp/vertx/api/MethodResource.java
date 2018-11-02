package com.github.ixtf.japp.vertx.api;

import com.github.ixtf.japp.vertx.Jvertx;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.ws.rs.*;
import java.lang.reflect.Method;

/**
 * @author jzb 2018-10-27
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MethodResource {
    @EqualsAndHashCode.Include
    private final ClassResource classResource;
    @ToString.Include
    @EqualsAndHashCode.Include
    private final Method method;
    @ToString.Include
    @EqualsAndHashCode.Include
    private final Path pathAnnotation;

    public MethodResource(ClassResource classResource, Method method) {
        this.classResource = classResource;
        this.method = method;
        pathAnnotation = method.getAnnotation(Path.class);
    }

    public ApiRoute getApiRoute() {
        final String path = getRouterPath();
        if (method.getAnnotation(POST.class) != null) {
            return new PostRoute(path, method);
        } else if (method.getAnnotation(PUT.class) != null) {
            return new PutRoute(path, method);
        } else if (method.getAnnotation(GET.class) != null) {
            return new GetRoute(path, method);
        } else if (method.getAnnotation(DELETE.class) != null) {
            return new DeleteRoute(path, method);
        }
        throw new RuntimeException(toString());
    }

    public String getRouterPath() {
        return classResource.getRouterPath() + Jvertx.getRouterPath(pathAnnotation);
    }

}
