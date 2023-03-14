package com.atguigu.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// 订单确认页需要用到的数据
public class OrderConfirmVo {
  // 收货地址，ums_member_receive_address 表
  @Getter
  @Setter
  private List<MemberAddressVo> address;

  // 购物清单，根据购物车页面传递过来的 skuIds 查询。 所有选中的购物项
  @Getter
  @Setter
  private List<OrderItemVO> items;

  // 防重令牌  (防止多次点击提交)
  @Getter
  @Setter
  private String orderToken;

  //  积分
  @Getter
  @Setter
  private Integer integration;
  // 发票记录...
  // 优惠劵消息...

  //  查询库存状态
  @Getter
  @Setter
  Map<Long, Boolean> stocks;

  private Integer count; // 数量
  private BigDecimal total; // 订单总额
  private BigDecimal payPrice; // 应付价格


  public Integer getCount() {
    Integer i = 0;
    if (items != null) {
      for (OrderItemVO item : items) {
        i += item.getCount();
      }
    }
    return i;
  }

  public BigDecimal getTotal() {
    BigDecimal sum = new BigDecimal("0");
    if (items != null) {
      for (OrderItemVO item : items) {
        BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
        sum = sum.add(multiply);
      }
    }
    return sum;
  }

  public BigDecimal getPayPrice() {
    return getTotal();
  }

}
