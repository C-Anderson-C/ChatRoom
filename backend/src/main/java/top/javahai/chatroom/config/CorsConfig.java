package top.javahai.chatroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类
 *
 * @author Hai
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  /**
   * 添加CORS映射
   * 通过WebMvcConfigurer接口实现全局CORS配置
   *
   * @param registry CORS注册表
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**") // 应用到所有路径
            // 添加多个允许的源
            .allowedOriginPatterns("http://localhost:8083", "http://localhost:8082", "http://localhost:8081", "http://127.0.0.1:8083")
            .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
            .allowedHeaders("*") // 允许所有头信息
            .exposedHeaders("Authorization", "Content-Type") // 暴露这些响应头
            .allowCredentials(true) // 允许发送认证信息（cookies等）
            .maxAge(3600); // 预检请求的有效期（秒）
  }

  /**
   * 创建CORS过滤器
   * 通过Filter方式实现CORS配置，当Spring Security启用时特别有用
   *
   * @return CorsFilter 跨域过滤器实例
   */
  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();

    // 配置允许的源 - 使用allowedOriginPatterns而不是allowedOrigins
    config.addAllowedOriginPattern("http://localhost:8083");
    config.addAllowedOriginPattern("http://localhost:8082");
    config.addAllowedOriginPattern("http://localhost:8081");
    config.addAllowedOriginPattern("http://127.0.0.1:8083");

    // 允许所有头信息
    config.addAllowedHeader("*");

    // 暴露一些响应头
    config.addExposedHeader("Authorization");
    config.addExposedHeader("Content-Type");

    // 允许所有HTTP方法
    config.addAllowedMethod("*");

    // 允许携带认证信息
    config.setAllowCredentials(true);

    // 配置预检请求的缓存时间
    config.setMaxAge(3600L);

    // 为所有路径应用这些CORS配置
    source.registerCorsConfiguration("/**", config);

    return new CorsFilter(source);
  }
}