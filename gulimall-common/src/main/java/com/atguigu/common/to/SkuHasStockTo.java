package com.atguigu.common.to;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SkuHasStockTo {
  private Long skuId;
  private Boolean hasStock;
}
