package test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
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
        Flux.interval(Duration.ofSeconds(5))
                .map(__ -> vertx.eventBus().request("test", null))
                .map(Future::toCompletionStage)
                .flatMap(Mono::fromCompletionStage)
                .map(Message::body)
                .onErrorContinue((e, o) -> e.printStackTrace())
                .subscribe(System.out::println);
    }

}
