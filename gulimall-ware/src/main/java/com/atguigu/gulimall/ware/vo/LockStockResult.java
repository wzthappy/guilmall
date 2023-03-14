package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResult {
  private Long skuId; // 商品id
  private Integer num; // 锁定的商品个数
  private Boolean locked; // 是否锁成功
}
