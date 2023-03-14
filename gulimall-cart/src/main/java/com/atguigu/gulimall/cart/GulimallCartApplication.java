package com.atguigu.gulimall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients  // 开启feign功能
@SpringBootApplication
@EnableDiscoveryClient  // 开启nacos服务注册功能
public class GulimallCartApplication {
  public static void main(String[] args) {
    SpringApplication.run(GulimallCartApplication.class, args);
  }
}
