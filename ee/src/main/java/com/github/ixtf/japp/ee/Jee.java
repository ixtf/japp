package com.github.ixtf.japp.ee;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

public class Jee {
    private static BeanManager _manager;

    public static <T> T getSingle(TypedQuery<T> q) {
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public static final BeanManager getBeanManager() {
        if (_manager != null) {
            return _manager;
        }
        return Optional.ofNullable(CDI.current())
                .map(CDI::getBeanManager)
                .orElseGet(() -> {
                    try {
                        final InitialContext initialContext = new InitialContext();
                        _manager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                        return _manager;
                    } catch (NamingException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    public static final <T> T getBean(Class<T> clazz, Annotation... qualifiers) {
        BeanManager beanManager = getBeanManager();
        Set<Bean<?>> set = beanManager.getBeans(clazz, qualifiers);
        for (Bean bean : set) {
            Context context = beanManager.getContext(bean.getScope());
            return (T) context.get(bean, beanManager.createCreationalContext(bean));
        }
        throw new RuntimeException();
    }

    public static final <T> void inject(Object obj) {
        inject(getBeanManager(), obj);
    }

    public static final <T> void inject(BeanManager beanManager, Object obj) {
        // CDI uses an AnnotatedType object to read the annotations of a class
        AnnotatedType<T> type = (AnnotatedType<T>) beanManager.createAnnotatedType(obj.getClass());

        // The extension uses an InjectionTarget to delegate instantiation,
        // dependency injection
        // and lifecycle callbacks to the CDI container
        InjectionTarget<T> it = beanManager.createInjectionTarget(type);
        // each instance needs its own CDI CreationalContext
        CreationalContext instanceContext = beanManager.createCreationalContext(null);

        it.inject((T) obj, instanceContext); // call initializer methods and
        // perform field injection
        it.postConstruct((T) obj); // call the @PostConstruct method
    }
}
