package com.atguigu.gulimall.seckill.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRabbitConfig {

  @Bean // MQ 使用JSON序列化机制，进行消息转换
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

}
