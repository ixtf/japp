package com.github.ixtf.japp.vertx.api;

import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.ws.rs.PUT;
import java.lang.reflect.Method;

/**
 * @author jzb 2018-10-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PutRoute extends ApiRoute {

    PutRoute(String path, Method method) {
        super(path, method);
    }

    @Override
    protected String addressPrefix() {
        return PUT.class.getSimpleName();
    }

    @Override
    public Route route(Router router) {
        return router.put(getPath());
    }
}
