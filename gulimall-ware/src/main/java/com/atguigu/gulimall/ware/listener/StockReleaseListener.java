package com.atguigu.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {
  @Autowired
  private WareSkuService wareSkuService;

  /**
   * 库存自动解锁:
   * 1. 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚之前锁定的库存就要自动解锁。
   * 2. 锁库存失败
   * <p>
   * 只要解锁库存的消息失败。一定要告诉服务器解锁失败。 ACK
   */
  @RabbitHandler
  public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
    try {
      System.out.println("收到解锁库存的消息");
      wareSkuService.unlockStock(to);
      channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
      channel.basicNack(message.getMessageProperties().getDeliveryTag(), true, false);
    }
  }

  @RabbitHandler
  public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
    System.out.println("订单关闭准备解锁库存...");
    try {
      wareSkuService.unlockStock(orderTo);
      channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
      channel.basicNack(message.getMessageProperties().getDeliveryTag(), true, false);
    }
  }
}
