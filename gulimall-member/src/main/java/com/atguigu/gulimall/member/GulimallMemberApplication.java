package com.atguigu.gulimall.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1. 想要远程调用别的服务
 *  1)、引入open-feign
 *  2)、编写一个接口，告诉SpringCloud这个接口需要调用远程访问
 *    1、声明接口的每一个方法都是调用那个远程服务的那个请求
 *    2、开启远程调用功能
 */
@SpringBootApplication
@EnableRedisHttpSession // 开启SpringSession统一存储功能
@EnableFeignClients(basePackages = "com.atguigu.gulimall.member.feign")  // 开启feign  远程调用功能
@EnableDiscoveryClient  // 开启nocos
public class GulimallMemberApplication {
  public static void main(String[] args) {
    SpringApplication.run(GulimallMemberApplication.class, args);
  }

}
