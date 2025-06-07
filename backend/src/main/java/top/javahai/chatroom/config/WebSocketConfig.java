package top.javahai.chatroom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  /**
   * 注册stomp站点
   * @param registry
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 修改为统一端点 /ws，移除 /ep
    registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
  }

  /**
   * 配置消息代理
   * @param registry
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 添加应用目的地前缀 /app
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic", "/queue");
  }
}