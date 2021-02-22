package test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

public class ZookeeperVerticle extends AbstractVerticle {
    public static void main(String[] args) {
        final var zkConfig = new JsonObject()
                .put("zookeeperHosts", "eq.medipath.com.cn")
                .put("retry", new JsonObject()
                        .put("initialSleepTime", 3000)
                        .put("maxTimes", 3)
                );
        final var mgr = new ZookeeperClusterManager(zkConfig);
        final var options = new VertxOptions().setClusterManager(mgr);
        Vertx.clusteredVertx(options).flatMap(vertx -> {
            final var deploymentOptions = new DeploymentOptions();
            return vertx.deployVerticle(ZookeeperVerticle.class, deploymentOptions);
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                System.out.println("success");
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer("test", reply -> {
            final var ret = new JsonObject().put("test", "test");
            reply.reply(ret);
        });
    }
}
