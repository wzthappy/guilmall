package com.atguigu.gulimall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration  // 解决feign远程调用时丢失请求头(cookie等...)。   feign的拦截器
public class GuliFeignConfig {

  @Bean("requestInterceptor")
  public RequestInterceptor requestInterceptor() {
    return new RequestInterceptor() {
      @Override
      public void apply(RequestTemplate template) {
        // 1. 使用RequestContextHolder拿到刚进来的这个请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes)
            RequestContextHolder.currentRequestAttributes();
        if (attributes != null) {
          HttpServletRequest request = attributes.getRequest(); // 老请求
          if (request != null) {
            // 同步请求头数据。  (cookie)
            String cookie = request.getHeader("Cookie"); // 老请求的Cookie
            template.header("Cookie", cookie); // 把老请求的Cookie加入到新请求中
          }
        }
      }
    };
  }
}
