package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.UUID;

@Controller
public class HelloController {
  @GetMapping("/{page}.html")
  public String listPage(@PathVariable("page") String page) {
    return page;
  }

  @Autowired
  private RabbitTemplate rabbitTemplate;

  /**
   * 测试
   */
  @ResponseBody
  @GetMapping("/test/createOrder")
  public String createOrderTest() {
    // 订单下单成功
    OrderEntity entity = new OrderEntity();
    entity.setOrderSn(IdWorker.getTimeId());
    entity.setModifyTime(new Date());
    // 给MQ发送消息
    rabbitTemplate.convertAndSend("order-event-exchange",
        "order.create.order", entity);
    rabbitTemplate.convertAndSend("stock-event-exchange",
        "stock.locked", entity);
    return "ok";
  }
}
