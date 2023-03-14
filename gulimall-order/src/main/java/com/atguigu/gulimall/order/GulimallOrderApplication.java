package com.atguigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用RabbitMQ
 *      1. 引入amqp场景: RabbitAutoConfiguration就会自动生效
 *      2. 给容器中自动配置了
 *          RabbitTemplate、AmqpAdmin、CachingConnectionFactory、RabbitMessagingTemplate
 *          所有的属性都是
 *          @ConfigurationProperties(prefix = "spring.rabbitmq")
 *                 public class RabbitProperties {
 *      3. 给配置文件中配置 spring.rabbitmq 信息
 *      4. @EnableRabbit 开启功能
 *      5. 监听消息: 使用@RabbitListener: 必须有@EnableRabbit
 *   @RabbitListener: 可以在类上或方法上 (监听哪些队列即可 )
 *   @RabbitHandler: 标在方法上 (重载区分不同的消息)
 *
 * Seata控制分布式事务:
 *    1)、每一个微服务先必须创建undo_log
 *    2)、安装事务协调器
 *    3)、整合
 *        1. 导入依赖 spring-cloud-starter-alibaba-seata；  0.7.1
 *        2. 启动seata-server;
 *            registry.conf: 注册中心  type = "nacos"    配置中心  type = "nacos"
 *
 *
 */
@EnableRabbit  // 开启rabbitMQ功能
@EnableFeignClients  // 开启feign功能
@SpringBootApplication
@EnableDiscoveryClient  // 开启nacos服务注册功能
@EnableRedisHttpSession // 开启SpringSession统一存储功能
public class GulimallOrderApplication {

  public static void main(String[] args) {
    SpringApplication.run(GulimallOrderApplication.class, args);
  }

}
