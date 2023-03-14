package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
public class RabbitController {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @GetMapping("/sendMq")
  public String sendMq(@RequestParam(value = "num", defaultValue = "10") Integer num) {
    for (int i = 0; i < num; i++) {
      OrderEntity entity = new OrderEntity();
      entity.setOrderSn(UUID.randomUUID().toString());
      rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java"
          , entity, new CorrelationData(UUID.randomUUID().toString()));


      OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
      reasonEntity.setId(1L);
      reasonEntity.setCreateTime(new Date());
      reasonEntity.setName("中国" + " ===== " + i);     // hello32.java 没有这个路由键，交换机到队列会错误。验证回退模式
      rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java"
          , reasonEntity,  new CorrelationData(UUID.randomUUID().toString()));

    }
    return "ok";
  }
}
