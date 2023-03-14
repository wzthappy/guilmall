package com.atguigu.gulimall.seckill;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 1. 整合sentinel
 *    1)、导入依赖
 *    2)、下载sentinel控制台
 *    3)、配置sentinel控制台地址
 *    4)、在控制台调整参数。【默认所有的流程设置是保证在内存中，重启失效】
 *
 * 2. 每一个微服务都导入actuator，指标监控
 *
 * 3. 自定义sentinel流控返回数据
 */
@EnableFeignClients  // 开启feign功能
@EnableDiscoveryClient  // 开启nacos功能
@SpringBootApplication   // 排除数据库的功能
    (exclude = {DataSourceAutoConfiguration.class, DruidDataSourceAutoConfigure.class})
public class GulimallSeckillApplication {
  public static void main(String[] args) {
    SpringApplication.run(GulimallSeckillApplication.class, args);
  }

}
