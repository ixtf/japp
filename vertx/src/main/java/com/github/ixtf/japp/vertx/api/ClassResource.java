package com.github.ixtf.japp.vertx.api;

import com.github.ixtf.japp.vertx.Jvertx;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.ws.rs.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author jzb 2018-10-27
 */
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClassResource {
    @EqualsAndHashCode.Include
    private final MethodResource parent;
    @ToString.Include
    @EqualsAndHashCode.Include
    @Getter
    private final Class<?> clazz;
    private final Path pathAnnotation;
    @ToString.Include
    @Getter
    private final Collection<MethodResource> methodResources;

    public ClassResource(Class<?> clazz) {
        this(null, clazz);
    }

    public ClassResource(MethodResource parent, Class<?> clazz) {
        this.parent = parent;
        this.clazz = clazz;
        pathAnnotation = clazz.getAnnotation(Path.class);
        methodResources = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> Objects.nonNull(method.getAnnotation(Path.class)))
                .map(method -> new MethodResource(this, method))
                .collect(Collectors.toList());
    }

    public String getRouterPath() {
        final String path = Jvertx.getRouterPath(pathAnnotation);
        return parent == null ? path : parent.getRouterPath() + path;
    }
}
