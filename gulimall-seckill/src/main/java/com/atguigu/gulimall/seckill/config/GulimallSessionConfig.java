package com.atguigu.gulimall.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableRedisHttpSession
public class GulimallSessionConfig {

  @Bean // 设置JSessionId的域范围等等
  public CookieSerializer cookieSerializer () {
    DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
    // 设置JSessionId(cookie)的存活时间，默认是只要关闭了浏览器就会删除
//    cookieSerializer.setCookieMaxAge(60);
    // 设置JSessionId(cookie)的作用域，域名中只要是以gulimall.com的都会共享同一个JSessionId
    cookieSerializer.setDomainName("gulimall.com");
    // 修改默认的jsessionid的名称
    cookieSerializer.setCookieName("GuLiSession");
    return cookieSerializer;
  }

  @Bean // springSession存放session的值到redis中时，java序列化的值变为json值到redis中
  public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
    return new GenericJackson2JsonRedisSerializer();
  }
}
