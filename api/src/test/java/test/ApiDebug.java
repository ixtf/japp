package test;

import com.github.ixtf.api.ApiLauncher;
import com.github.ixtf.api.MainVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

@Slf4j
public class ApiDebug extends ApiLauncher {

    public static void main(String[] args) {
        System.setProperty("vertx.hazelcast.config", "/home/data/api/cluster.xml");
        System.setProperty("hazelcast.local.publicAddress", localIp());
        new ApiDebug().dispatch(new String[]{"-cluster"});
    }

    @Override
    protected String getMainVerticle() {
        return MainVerticle.class.getName();
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        super.afterStartingVertx(vertx);

        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start(Promise<Void> startPromise) throws Exception {
                vertx.eventBus().localConsumer("test1", reply -> {
                    final var context = vertx.getOrCreateContext();
                    context.put("test", "test1");
                    final var ret = new JsonObject().put("test1", Thread.currentThread().toString());
                    reply.reply(ret.toBuffer());
                });
                vertx.eventBus().localConsumer("test2", reply -> {
                    final var context = vertx.getOrCreateContext();
                    System.out.println((String) context.get("test"));
                    final var ret = new JsonObject().put("test2", Thread.currentThread().toString());
                    reply.reply(ret.toBuffer());
                });
            }
        }, new DeploymentOptions().setWorker(true));
    }

    @SneakyThrows({SocketException.class, UnknownHostException.class})
    private static String localIp() {
        @Cleanup final var socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        return socket.getLocalAddress().getHostAddress();
    }

}
