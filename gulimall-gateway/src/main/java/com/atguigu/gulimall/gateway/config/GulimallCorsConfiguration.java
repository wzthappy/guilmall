package com.atguigu.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

// 解决跨域的请求
@Configuration
public class GulimallCorsConfiguration {
  @Bean
  public CorsWebFilter corsWebFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    // 1. 配置跨域
    corsConfiguration.addAllowedHeader("*"); // 允许那些头跨域
    corsConfiguration.addAllowedMethod("*"); // 允许那些请求方式
    corsConfiguration.addAllowedOrigin("*"); // 允许那些请求来源跨域
    corsConfiguration.setAllowCredentials(true); // 跨域允许携带cooker消息

    source.registerCorsConfiguration("/**", corsConfiguration); // 表示任意路径都要进行跨域配置
    return new CorsWebFilter(source);
  }
}
