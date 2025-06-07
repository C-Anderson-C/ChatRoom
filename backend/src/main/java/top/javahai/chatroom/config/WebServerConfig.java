package top.javahai.chatroom.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerConfig {

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        // 配置 Tomcat
        factory.addConnectorCustomizers(connector -> {
            connector.setMaxPostSize(10485760); // 10MB
            connector.setProperty("maxHttpHeaderSize", "65536"); // 64KB
            connector.setProperty("maxSwallowSize", "-1");
        });

        return factory;
    }
}