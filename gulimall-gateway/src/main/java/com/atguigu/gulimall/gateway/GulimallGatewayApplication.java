package com.atguigu.gulimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1. 开启服务注册发现
 *   (配置nacos的注册中心地址)
 */
@EnableDiscoveryClient // 开启nacos的服务注册发现
@SpringBootApplication
public class GulimallGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(GulimallGatewayApplication.class, args);
  }

}
