package com.atguigu.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 注意:
 *    如果没有使用创建的mq，那么不会创建，直到第一次使用才会创建
 */
@Configuration
public class MyRabbitConfig {
//  @Value("${rabbit.ttl}")
//  private Integer ttl;

  @Bean // MQ 使用JSON序列化机制，进行消息转换
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean // 创建交换机
  public Exchange stcokEventExchange () {
    return new TopicExchange("stock-event-exchange", true, false);
  }
  @Bean // 创建队列
  public Queue stockReleaseStockQueue () {
    return new Queue("stock.release.stock.queue", true, false, false);
  }
  @Bean // 创建延迟队列
  public Queue  stockDelayQueue() {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("x-dead-letter-exchange", "stock-event-exchange");  // 交换机   信息死了发送的交换机
    arguments.put("x-dead-letter-routing-key", "stock.release");  // 路由key
    arguments.put("x-message-ttl", 120000); // 60000 -> 1分钟  过期时间
    return new Queue("stock.delay.queue", true, false, false, arguments);
  }
  @Bean // 绑定
  public Binding stockReleaseBinding() {
    return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE,
        "stock-event-exchange", "stock.release.#", null);
  }
  @Bean // 绑定
  public Binding stockLockedBinding() {
    return new Binding("stock.delay.queue", Binding.DestinationType.QUEUE,
        "stock-event-exchange", "stock.locked", null);
  }
}
