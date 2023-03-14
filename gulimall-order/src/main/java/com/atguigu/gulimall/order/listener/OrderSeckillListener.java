package com.atguigu.gulimall.order.listener;

import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RabbitListener(queues = "order.seckill.order.queue")
public class OrderSeckillListener {
  @Autowired
  private OrderService orderService;

  @RabbitHandler
  public void listener(SeckillOrderTo seckillOrder, Message mes , Channel channel) throws IOException {
    long deliveryTag = mes.getMessageProperties().getDeliveryTag();
    try {
      log.info("准备创建秒杀单的详细信息...");
      orderService.createSeckillOrder(seckillOrder);

      channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
      channel.basicNack(deliveryTag, true, false);
    }
  }
}
