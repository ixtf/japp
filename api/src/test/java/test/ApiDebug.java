package test;

import com.github.ixtf.api.ApiLauncher;
import com.github.ixtf.api.MainVerticle;
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
//        System.setProperty("vertx.hazelcast.config", "/home/data/api/cluster.xml");
//        System.setProperty("hazelcast.local.publicAddress", localIp());
//        new ApiDebug().dispatch(new String[]{"-cluster"});
        new ApiDebug().dispatch(args);
    }

    @Override
    protected String getMainVerticle() {
        return MainVerticle.class.getName();
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        super.afterStartingVertx(vertx);

        vertx.eventBus().consumer("test:test", reply -> {
            final var ret = new JsonObject().put("test", "test");
            reply.reply(ret.toBuffer());
//            reply.reply(ret.encode());
        });
    }

    @SneakyThrows({SocketException.class, UnknownHostException.class})
    private static String localIp() {
        @Cleanup final var socket = new DatagramSocket();
        socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
        return socket.getLocalAddress().getHostAddress();
    }

}
