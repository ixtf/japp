package com.github.ixtf.persistence.api;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.ixtf.J;
import com.github.ixtf.persistence.IEntity;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.commons.lang3.reflect.MethodUtils.getMatchingMethod;
import static org.apache.commons.lang3.reflect.MethodUtils.getMethodsListWithAnnotation;

/**
 * @author jzb 2019-02-18
 */
public abstract class AbstractUnitOfWork implements UnitOfWork {
    private static final LoadingCache<Class<? extends IEntity>, Multimap<Class<? extends Annotation>, PersistenceCallback>> cache = Caffeine.newBuilder().build(new CacheLoader<>() {
        @Override
        public Multimap<Class<? extends Annotation>, PersistenceCallback> load(Class<? extends IEntity> entityClass) throws Exception {
            final Multimap<Class<? extends Annotation>, PersistenceCallback> multimap = ArrayListMultimap.create();
            Stream<PersistenceCallback> stream = streamAnnotation().flatMap(it -> callbackStream(entityClass, it));
            final EntityListeners entityListeners = entityClass.getAnnotation(EntityListeners.class);
            if (entityListeners != null) {
                for (Class listenerClass : entityListeners.value()) {
                    final Object listener = listenerClass.getDeclaredConstructor().newInstance();
                    final Stream<PersistenceCallback> stream1 = streamAnnotation().flatMap(it -> callbackStream(entityClass, it, listenerClass, listener));
                    stream = Stream.concat(stream, stream1);
                }
            }
            stream.forEach(it -> multimap.put(it.getAnnotationClass(), it));
            return multimap;
        }

        private Stream<Class<? extends Annotation>> streamAnnotation() {
            return Stream.of(PrePersist.class, PostPersist.class, PreUpdate.class, PostUpdate.class, PreRemove.class, PostRemove.class).distinct();
        }

        private Stream<PersistenceCallback> callbackStream(Class<? extends IEntity> entityClass, Class<? extends Annotation> annotationClass) {
            return getMethodsListWithAnnotation(entityClass, annotationClass, false, true).stream()
                    .filter(method -> {
                        final Method matchingMethod = getMatchingMethod(entityClass, method.getName());
                        return Objects.equals(method, matchingMethod);
                    })
                    .map(method -> new EntitySelfHandler(entityClass, annotationClass, method));
        }

        private Stream<PersistenceCallback> callbackStream(Class<? extends IEntity> entityClass, Class<? extends Annotation> annotationClass, Class<?> listenerClass, Object listener) {
            return getMethodsListWithAnnotation(listenerClass, annotationClass, false, true).stream()
                    .filter(method -> {
                        final Method matchingMethod = getMatchingMethod(listenerClass, method.getName(), entityClass);
                        return Objects.equals(method, matchingMethod);
                    })
                    .map(method -> new EntityListenerHandler(entityClass, annotationClass, listenerClass, listener, method));
        }
    });
    protected final List<IEntity> newList = Collections.synchronizedList(Lists.newArrayList());
    protected final List<IEntity> dirtyList = Collections.synchronizedList(Lists.newArrayList());
    protected final List<IEntity> cleanList = Collections.synchronizedList(Lists.newArrayList());
    protected final List<IEntity> deleteList = Collections.synchronizedList(Lists.newArrayList());

    protected abstract boolean exists(IEntity o);

    @Override
    synchronized public UnitOfWork registerSave(IEntity o) {
        if (exists(o)) {
            registerDirty(o);
        } else {
            registerNew(o);
        }
        return this;
    }

    @Override
    synchronized public UnitOfWork registerNew(IEntity o) {
        if (!newList.contains(o)) {
            newList.add(o);
        }
        return this;
    }

    @Override
    synchronized public UnitOfWork registerDirty(IEntity o) {
        if (!dirtyList.contains(o)) {
            dirtyList.add(o);
        }
        return this;
    }

    @Override
    synchronized public UnitOfWork registerClean(IEntity o) {
        if (!cleanList.contains(o)) {
            cleanList.add(o);
        }
        return this;
    }

    @Override
    synchronized public UnitOfWork registerDelete(IEntity o) {
        if (!deleteList.contains(o)) {
            deleteList.add(o);
            newList.remove(o);
            dirtyList.remove(o);
            cleanList.remove(o);
        }
        return this;
    }

    @SneakyThrows
    protected Stream<PersistenceCallback> callbackStream(IEntity o, Class<? extends Annotation> annotationClass) {
        return callbackStream(J.actualClass(o.getClass()), annotationClass);
    }

    @SneakyThrows
    protected Stream<PersistenceCallback> callbackStream(Class<? extends IEntity> entityClass, Class<? extends Annotation> annotationClass) {
        final Multimap<Class<? extends Annotation>, PersistenceCallback> multimap = cache.get(entityClass);
        return multimap.get(annotationClass).stream();
    }

    private static class EntitySelfHandler implements PersistenceCallback {
        @Getter
        private final Class<? extends IEntity> entityClass;
        @Getter
        private final Class<? extends Annotation> annotationClass;
        private final Method method;

        private EntitySelfHandler(Class<? extends IEntity> entityClass, Class<? extends Annotation> annotationClass, Method method) {
            this.entityClass = entityClass;
            this.annotationClass = annotationClass;
            this.method = method;
            method.setAccessible(true);
        }

        @SneakyThrows
        @Override
        public void callback(IEntity o) {
            method.invoke(o);
        }
    }

    private static class EntityListenerHandler implements PersistenceCallback {
        @Getter
        private final Class<? extends IEntity> entityClass;
        @Getter
        private final Class<? extends Annotation> annotationClass;
        private final Class<?> listenerClass;
        private final Object listener;
        private final Method method;

        private EntityListenerHandler(Class<? extends IEntity> entityClass, Class<? extends Annotation> annotationClass, Class<?> listenerClass, Object listener, Method method) {
            this.entityClass = entityClass;
            this.annotationClass = annotationClass;
            this.listenerClass = listenerClass;
            this.listener = listener;
            this.method = method;
            method.setAccessible(true);
        }

        @SneakyThrows
        @Override
        public void callback(IEntity o) {
            method.invoke(listener, o);
        }
    }

}
