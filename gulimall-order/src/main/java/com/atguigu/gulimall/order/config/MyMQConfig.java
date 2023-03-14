package com.atguigu.gulimall.order.config;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: springCloud版本过低，使用使用这种写法  -_-
 *    RabbitMq 只要有。@Bean声明的属性方式变化，对应的队列和交换机不会发生变化
 */
@Configuration
public class MyMQConfig {

  //  @Value("${rabbit.ttl}")
//  private Integer ttl;

  @Bean  // 创建交换机
  public Exchange orderEventExchange() {
    return new TopicExchange("order-event-exchange", true, false);
  }

  @Bean // 创建队列  (延迟队列)
  public Queue orderDelayQueue() {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("x-dead-letter-exchange", "order-event-exchange");  // 交换机
    arguments.put("x-dead-letter-routing-key", "order.release.order");  // 路由key
    arguments.put("x-message-ttl", 60000); // 60000 -> 1分钟  过期时间
    Queue queue = new Queue("order.delay.queue", true,
        false, false, arguments);
    return queue;
  }

  @Bean // 创建死信队列
  public Queue orderReleaseOrderQueue() {
    Queue queue = new Queue("order.release.order.queue", true,
        false, false);
    return queue;
  }

  @Bean // 交换机 和 延迟队列  绑定
  public Binding orderCreateBingding() {
    return new Binding("order.delay.queue", Binding.DestinationType.QUEUE,
        "order-event-exchange", "order.create.order", null);
  }

  @Bean // 交换机 和 死信队列  绑定
  public Binding orderCreateOrderBingding() {
    return  new Binding("order.release.order.queue", Binding.DestinationType.QUEUE,
        "order-event-exchange", "order.release.order", null);
  }

  /**
   * 订单释放直接和库存释放进行绑定
   */
  @Bean // 订单交换机  和  库存队列  绑定
  public Binding orderReleaseOtherBingding (){
    return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE,
        "order-event-exchange", "order.release.other.#", null);
  }

  @Bean
  public Queue orderSeckillOrderQueue() {
    return new Queue("order.seckill.order.queue", true, false, false);
  }

  @Bean
  public Binding orderSeckillOrderQueueBinding() {
    return new Binding(
        "order.seckill.order.queue", Binding.DestinationType.QUEUE,
        "order-event-exchange", "order.seckill.order", null);
  }
}
