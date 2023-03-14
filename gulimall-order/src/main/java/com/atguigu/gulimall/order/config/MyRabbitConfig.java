package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

//  @Autowired
  private RabbitTemplate rabbitTemplate;

  // TDOD
  @Bean
  @Primary
  public RabbitTemplate rabbitTemplate (ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    this.rabbitTemplate = rabbitTemplate;
    rabbitTemplate.setMessageConverter(messageConverter());
    initRabbitTemplate();
    return rabbitTemplate;
  }

  @Bean // MQ 使用JSON序列化机制，进行消息转换
  public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * 定制RabbitTemplate
   * 1. 服务接收到消息就回调:
   * 设置确认模式
   * spring:rabbitmq:publisher-confirms: true
   * 2.消息正确抵达队列进行回调:
   * 设置回退模式
   * spring:rabbitmq:publisher-returns: true  # 开启回退模式 (交换机 -> 队列)
   * spring:rabbitmq:template:mandatory: true # 只要抵达队列，就以异步的发送优先回调我们这个returnConfirm
   * 3. 消费端确认 (保证每个消息被正确消费，此时才可以broker删除这个消息)
   * spring:rabbitmq:listener:simple:acknowledge-mode: manual
   * 默认是自动确认的，只要消息接收到，客户端会自动确认，服务端就会移除这个消息
   * 问题: 我们收到很多消息，自动回复给服务器ack，只有一个消息处理成功，宕机了。发生消息丢失；
   * 消费者手动确认模式下，只要我们没有明确告诉MQ，货物被签收。没有ACK，
   * 消息就一直是unacked状态。即使Consume宕机。消息不会丢失，会重新变为Ready，下一次有新的Consumer连接进来就发给他
   * 如何签收:
   * channel.basicAck(deliveryTag, false);  签收货物
   * channel.basicNack(deliveryTag, false, true);  拒签；业务处理失败
   */
//  @PostConstruct  // MyRabbitConfig对象创建完成以后，执行这个方法
  public void initRabbitTemplate() {
    // 确认回调
    rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
      /**
       * 1. 只要消息抵达Broker就ack=true
       * @param correlationData 当前消息的唯一关联数据 (这个是消息的唯一id)
       * @param ack 消息是否成功收到
       * @param cause 失败的原因
       */
      @Override
      public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        /**
         * 1. 做好消息确认机制(pulisher, consumer【手动ack】)
         * 2. 每一个发送的消息都在数据库做好记录。定期将失败的消息在次发送一遍
         */
        // 服务器收到了
        // 修改消息的状态
        System.out.println("confirm....correlationData[" + correlationData + "]" +
            "===>ack[" + ack + "]===>cause[" + cause + "]");
      }
    });

    // 回退回调
    rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
      /**
       * 只要消息没有投递给指定的队列，就触发这个失败回调
       * @param message 投递失败的详细消息
       * @param replyCode 回复的状态码.
       * @param replyText 回复的文本内容
       * @param exchange 当前这个消息发给那个交换机
       * @param routingKey 当时这个消息使用的那个路由键
       */
      @Override
      public void returnedMessage(Message message, int replyCode, String replyText,
                                  String exchange, String routingKey) {
        // 报错误了。修改数据库当前消息的状态 -> 错误
        System.out.println("Fail Message[" + message + "]===>replyCode[" + replyCode + "]===>" +
            "replyText[" + replyText + "]===>exchange[" + exchange + "]" +
            "===>routingKey[" + routingKey + "]");
      }
    });


  }
}
