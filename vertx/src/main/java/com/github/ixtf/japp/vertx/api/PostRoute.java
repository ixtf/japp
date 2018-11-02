package com.github.ixtf.japp.vertx.api;

import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.ws.rs.POST;
import java.lang.reflect.Method;

/**
 * @author jzb 2018-10-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostRoute extends ApiRoute {

    PostRoute(String path, Method method) {
        super(path, method);
    }

    @Override
    protected String addressPrefix() {
        return POST.class.getSimpleName();
    }

    @Override
    public Route route(Router router) {
        return router.post(getPath());
    }
}
