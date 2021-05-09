package test;


import com.google.common.io.Resources;
import io.smallrye.config.ConfigValuePropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Properties;

public class TestConfig {
    @SneakyThrows
    public static void main(String[] args) {
        final var properties = new Properties();
        System.out.println(ConfigProvider.getConfig());

        final var resource = Resources.getResource("test/application.properties");
        final var configSource = new ConfigValuePropertiesConfigSource(resource);
        final var config = new SmallRyeConfigBuilder()
                .withSources(configSource)
                .withProfile("test")
                .build();
        final var value = config.getValue("mp.openapi.extensions.smallrye.info.title", String.class);
        System.out.println(value);
    }
}
