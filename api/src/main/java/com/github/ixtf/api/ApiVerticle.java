package com.github.ixtf.api;

import com.github.ixtf.api.proxy.KeycloakService;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.ClusterHealthCheck;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.time.Duration;
import java.util.HashMap;

import static com.github.ixtf.api.ApiModule.*;
import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.opentracing.propagation.Format.Builtin.TEXT_MAP;
import static io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth.discover;
import static java.util.Optional.ofNullable;

@Slf4j
public class ApiVerticle extends AbstractVerticle implements Handler<RoutingContext> {
    private static final long DL_TIMEOUT = Duration.ofMinutes(5).toMillis();
    private static final String KeycloakAdmin = "__com.github.ixtf.api:KeycloakAdmin__";
    @Inject
    private Tracer tracer;
    @Inject
    private OAuth2Options oAuth2Options;
    @Inject
    private CorsHandler corsHandler;
    @Inject
    private KeycloakService keycloakService;

    public static String apiAddress(RoutingContext rc) {
        final var service = rc.pathParam("service");
        final var action = rc.pathParam("action");
        return String.join(":", service, action);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        injectMembers(this);

        new ServiceBinder(vertx).register(KeycloakService.class, keycloakService);
        vertx.eventBus().consumer(KeycloakAdmin, reply -> reply.reply(getInstance(JsonObject.class, CONFIG).getJsonObject("keycloak-admin", new JsonObject())));

        discover(vertx, oAuth2Options).flatMap(this::createHttpServer).<Void>mapEmpty().onComplete(startPromise);
    }

    private Future<HttpServer> createHttpServer(final OAuth2Auth oAuth2Auth) {
        final var oAuth2AuthHandler = OAuth2AuthHandler.create(vertx, oAuth2Auth);
        final var router = Router.router(vertx);
        router.route().handler(corsHandler);
        router.route().handler(BodyHandler.create());
        router.route("/metrics").handler(PrometheusScrapingHandler.create());
        router.route("/health*").handler(HealthCheckHandler.create(vertx));
        router.route("/ping*").handler(HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx)));
        final var procedure = ClusterHealthCheck.createProcedure(vertx);
        final var healthChecks = HealthChecks.create(vertx).register("cluster-health", procedure);
        router.get("/readiness").handler(HealthCheckHandler.createWithHealthChecks(healthChecks));

        router.route().failureHandler(rc -> {
            final var errMsg = new JsonObject().put("errMsg", rc.failure().getMessage());
            rc.response().setStatusCode(400).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(errMsg.encode());
        });
        // todo add auth fix sockJSAddressRegex in config
        final var permitted = new PermittedOptions().setAddressRegex("medipath://ws/.+");
        final var sockJSBridgeOptions = new SockJSBridgeOptions().addOutboundPermitted(permitted);
        router.mountSubRouter("/eventbus", SockJSHandler.create(vertx).bridge(sockJSBridgeOptions));

        router.route("/test/:address").handler(rc -> vertx.eventBus().<Buffer>request(rc.pathParam("address"), null).onComplete(ar -> {
            if (ar.succeeded()) {
                final var body = ar.result().body();
                rc.response().end(body);
            } else {
                rc.fail(ar.cause());
            }
        }));
        router.route("/api/services/:service/actions/:action").handler(oAuth2AuthHandler).handler(this);
        router.route("/dl/services/:service/actions/:action/tokens/:token").handler(this);

        final var httpServerOptions = new HttpServerOptions().setCompressionSupported(true);
        return vertx.createHttpServer(httpServerOptions).requestHandler(router).listen(9998);
    }

    @Override
    public void handle(RoutingContext rc) {
        Future.<Message<Object>>future(p -> {
            final var deliveryOptions = deliveryOptions(rc);
            final var token = rc.pathParam("token");
            final Object body;
            if (token == null) {
                body = rc.getBody();
                final var principal = rc.user().attributes().getString("sub");
                deliveryOptions.addHeader(Principal.class.getName(), principal);
                rc.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
            } else {
                body = token;
                deliveryOptions.setSendTimeout(Math.max(DL_TIMEOUT, deliveryOptions.getSendTimeout()));
                rc.response().putHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            }
            vertx.eventBus().request(apiAddress(rc), body, deliveryOptions, p);
        }).onComplete(ar -> {
            if (ar.failed()) {
                onFailure(rc, ar.cause());
            } else {
                onSuccess(rc, ar.result());
            }
            tracer.activeSpan().setTag(Tags.HTTP_STATUS, rc.response().getStatusCode()).finish();
        });
    }

    private void onSuccess(RoutingContext rc, Message<Object> message) {
        final var response = rc.response();
        message.headers().forEach(it -> {
            if (it.getKey().equals(HttpResponseStatus.class.getName())) {
                final var status = HttpResponseStatus.parseLine(it.getValue());
                response.setStatusCode(status.code());
            } else {
                response.putHeader(it.getKey(), it.getValue());
            }
        });
        final var statusCode = response.getStatusCode();
        if (statusCode >= 300 && statusCode < 400) {
            response.end();
        } else {
            final var body = message.body();
            if (body == null) {
                response.end();
            } else if (body instanceof Buffer) {
                final var buffer = (Buffer) body;
                response.end(buffer);
            } else if (body instanceof byte[]) {
                final var bytes = (byte[]) body;
                response.end(Buffer.buffer(bytes));
            } else if (body instanceof String) {
                final var s = (String) body;
                response.putHeader(CONTENT_TYPE, TEXT_PLAIN).end(s);
            } else {
                onFailure(rc, new RuntimeException("body must be (null | Buffer | byte[] | String)"));
            }
        }
    }

    private void onFailure(RoutingContext rc, Throwable e) {
        rc.fail(e);
        log.error(apiAddress(rc), e);
        tracer.activeSpan().setTag(Tags.ERROR, true).log(e.getMessage());
    }

    private DeliveryOptions deliveryOptions(RoutingContext rc) {
        final var deliveryOptions = new DeliveryOptions().setTracingPolicy(TracingPolicy.ALWAYS);
        ofNullable(activateSpan(rc)).map(Span::context).ifPresent(it -> {
            final var map = new HashMap<String, String>();
            tracer.inject(it, TEXT_MAP, new TextMapAdapter(map));
            map.forEach((k, v) -> deliveryOptions.addHeader(k, v));
        });
        ofNullable(rc.queryParam("timeout"))
                .filter(it -> it.size() > 0)
                .map(it -> it.get(0))
                .map(Long::parseLong)
                .filter(it -> it > DeliveryOptions.DEFAULT_TIMEOUT)
                .ifPresent(deliveryOptions::setSendTimeout);
        final var authorization = AUTHORIZATION.toString(0);
        rc.request().headers().forEach(it -> {
            final var key = it.getKey().toLowerCase();
            if (!authorization.equalsIgnoreCase(it.getKey())) {
                deliveryOptions.addHeader(key, it.getValue());
            }
        });
        return deliveryOptions;
    }

    // todo 使用vertx TracingOptions
    private Span activateSpan(RoutingContext rc) {
        final var request = rc.request();
        final var operationName = request.method().name();
        final var spanBuilder = tracer.buildSpan(operationName);
        final var map = new HashMap<String, String>();
        request.headers().forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        ofNullable(tracer.extract(TEXT_MAP, new TextMapAdapter(map))).ifPresent(spanBuilder::asChildOf);
        final var span = spanBuilder.start()
                .setTag(Tags.HTTP_METHOD, operationName)
                .setTag(Tags.HTTP_URL, request.uri());
        tracer.activateSpan(span);
        rc.put(Span.class.getName(), span);
        return span;
    }
}
