package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVO {
  private Long skuId; // 商品Id
  private String title; // 商品的标题
  private String image; // 商品的图片
  private List<String> skuAttr; // 商品的规格(属性)
  private BigDecimal price; // 价格
  private Integer count; // 数量
  private BigDecimal totalPrice; // 总价格

  private BigDecimal weight; // 商品重量
}
