package com.atguigu.gulimall.cart.config;


import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {
  @Override
  public void addInterceptors(InterceptorRegistry registry) { // 添加拦截器
    registry.addInterceptor(new CartInterceptor()).addPathPatterns("/**"); // 表示这个服务的所有请求都必须经过这个拦截器
  }
}
