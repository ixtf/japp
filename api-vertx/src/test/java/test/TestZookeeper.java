package test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

public class TestZookeeper {
    public static void main(String[] args) {
        final var zkConfig = new JsonObject()
                .put("zookeeperHosts", "eq.medipath.com.cn")
                .put("retry", new JsonObject()
                        .put("initialSleepTime", 3000)
                        .put("maxTimes", 3)
                );
        final var mgr = new ZookeeperClusterManager(zkConfig);
        final var options = new VertxOptions().setClusterManager(mgr);
        Vertx.clusteredVertx(options).onSuccess(TestZookeeper::testVertx);
    }

    private static void testVertx(Vertx vertx) {
        vertx.eventBus().request("test", null).onComplete(ar -> {
            if (ar.succeeded()) {
                final var result = ar.result();
                System.out.println(result);
            } else {
                ar.cause().printStackTrace();
            }
        });
    }

}
