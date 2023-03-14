package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 *    需要计算的属性，必须重写他的get方法，每次保证获取属性都会进行计算
 */
public class Cart {
  private List<CartItem> items;
  private Integer countNum; // 商品的总数量
  private Integer countType; // 商品种类数量
  private BigDecimal totalAmount; // 商品总价格
  private BigDecimal reduce = new BigDecimal("0.00"); // 优惠价格

  public Integer getCountNum() {
    int count = 0;
    if (this.items != null && this.items.size() > 0) {
      for (CartItem item : this.items) {
        count += item.getCount();
      }
    }
    return count;
  }

  public Integer getCountType() {
    return this.items.size();
  }


  public BigDecimal getTotalAmount() {
    BigDecimal amount = new BigDecimal("0");
    // 1. 计算购物项总价
    if (this.items != null && this.items.size() > 0) {
      for (CartItem item : this.items) {
        if (item.getCheck()) {
          BigDecimal totalPrice = item.getTotalPrice();
          amount = amount.add(totalPrice);
        }
      }
    }
    // 2. 减去优惠总价
    BigDecimal subtract = amount.subtract(getReduce());
    return subtract;
  }

  public BigDecimal getReduce() {
    return reduce;
  }

  public void setReduce(BigDecimal reduce) {
    this.reduce = reduce;
  }

  public List<CartItem> getItems() {
    return items;
  }

  public void setItems(List<CartItem> items) {
    this.items = items;
  }

}
