package com.atguigu.gulimall.order.service.impl;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.service.OrderItemService;


@Service("orderItemService")
@RabbitListener(queues = "hello-java-queue")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<OrderItemEntity> page = this.page(
        new Query<OrderItemEntity>().getPage(params),
        new QueryWrapper<OrderItemEntity>()
    );

    return new PageUtils(page);
  }

  /**
   * queues: 说明需要监听的所有队列
   * class org.springframework.amqp.core.Message
   * <p>
   * 参数可以写以下类型:
   * 1. Message message: 原生详细消息。头+体
   * 2. T<发送的消息类型>
   * 3. Channel channel: 当前传输数据的通道
   * Queue: 可以很多人都来监听。只要收到消息，列表删除消息，而且只能有一个收到此消息
   * 场景:
   * 1) 订单服务启动多个; 同一个消息，只能有一个客户端收到
   * 2) 只有一个消息完全处理完，方法运行结束，我们就可以接收下一个消息
   */
//    @RabbitListener(queues = "hello-java-queue")
  @RabbitHandler
  public void recieveMessage(Message message, OrderReturnReasonEntity context,
                             Channel channel) throws InterruptedException, IOException {
    // channel内按顺序自增的
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    System.out.println(deliveryTag);
    try {
      System.out.println("==================");
      System.out.println("接收到消息...内容: ");
      System.out.println(context);
//        System.out.println("类型: ");
//        System.out.println(message.getClass());
      System.out.println("------------------");

      // 消息头属性消息
      MessageProperties properties = message.getMessageProperties();

      // 确认签收
      channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
      // 拒绝签收
      channel.basicNack(deliveryTag, false, true);
    }
  }

  @RabbitHandler
  public void recieveMessage2(Message message, OrderEntity context, Channel channel) throws InterruptedException, IOException {

    System.out.println("================");
    System.out.println("接收到消息...内容: ");
    System.out.println(context);
//        System.out.println("类型: ");
//        System.out.println(message.getClass());
    System.out.println("==================");
    // 确认签收
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    // 消息头属性消息

  }
}