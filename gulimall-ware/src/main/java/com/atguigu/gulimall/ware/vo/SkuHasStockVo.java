package com.atguigu.gulimall.ware.vo;

import lombok.ToString;

@ToString
public class SkuHasStockVo {
  private Long skuId;
  private Boolean hasStock;

  public Long getSkuId() {
    return skuId;
  }

  public void setSkuId(Long skuId) {
    this.skuId = skuId;
  }

  public Boolean getHasStock() {
    return hasStock;
  }

  public void setHasStock(Long hasStock) {
    this.hasStock = hasStock != null && hasStock > 0;
  }
}
