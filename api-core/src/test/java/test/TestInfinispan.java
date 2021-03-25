package test;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

public class TestInfinispan {
    public static void main(String[] args) {
        final var builder = new ConfigurationBuilder()
                .uri("hotrod://admin:pass@dev.medipath.com.cn:11222");
        final var remoteCacheManager = new RemoteCacheManager(builder.build());
        System.out.println(remoteCacheManager);
    }
}
