package com.atguigu.gulimall.order.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
@ToString
public class OrderSubmitVo {
  private Long addrId; // 收获地址id
  private Integer payType; // 支付方式
  // 无需提交购买的商品，去购物车在获取一遍
  // 优惠，发票
  private String orderToken; // 防重令牌
  private BigDecimal payPrice; // 应付价格  验价
  private String note; // 订单备注
  // 用户修改消息，直接去session取出登录的用户
}
