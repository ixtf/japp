package com.github.ixtf.japp.vertx.api;

import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;

import javax.ws.rs.GET;
import java.lang.reflect.Method;

/**
 * @author jzb 2018-10-27
 */
public class GetRoute extends ApiRoute {

    GetRoute(String path, Method method) {
        super(path, method);
    }

    @Override
    protected String addressPrefix() {
        return GET.class.getSimpleName();
    }

    @Override
    protected Route route(Router router) {
        return router.get(getPath());
    }

}
