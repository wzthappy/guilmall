package com.atguigu.gulimall.ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@MapperScan("com.atguigu.gulimall.ware.dao")
@EnableRabbit // 开启RabbitMq
@EnableDiscoveryClient // 开启服务注册功能
@EnableTransactionManagement // 开启事务功能
@EnableFeignClients  // 开启远程调用功能  Feign
@SpringBootApplication
public class GulimallWareApplication {

  public static void main(String[] args) {
    SpringApplication.run(GulimallWareApplication.class, args);
  }

}
