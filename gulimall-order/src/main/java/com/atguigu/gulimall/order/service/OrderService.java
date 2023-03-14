package com.atguigu.gulimall.order.service;

import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-15 14:53:45
 */
public interface OrderService extends IService<OrderEntity> {

  PageUtils queryPage(Map<String, Object> params);

  /**
   * 订单确认页返回需要用的数据
   */
  OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

  /**
   * 下单
   */
  SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

  OrderEntity getOrderByOrderSn(String orderSn);

  void closeOrder(OrderEntity entity);

  /**
   * 获取当前订单的支付信息
   * @param orderSn
   * @return
   */
  PayVo getOrderPay(String orderSn);

  PageUtils queryPageWithItem(Map<String, Object> params);

  String handlePayResult(PayAsyncVo vo);

  void createSeckillOrder(SeckillOrderTo seckillOrder);
}

