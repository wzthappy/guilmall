package com.atguigu.gulimall.member.config;

import com.atguigu.gulimall.member.interceptor.LoginUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MemberWebConfig implements WebMvcConfigurer {
  @Autowired
  private LoginUserInterceptor loginUserInterceptor;

  @Override // 添加/使用拦截器
  public void addInterceptors(InterceptorRegistry registry) {
    // 添加登录拦截器
    registry.addInterceptor(loginUserInterceptor).addPathPatterns("/**")
        .excludePathPatterns("/member/**");
  }
}
