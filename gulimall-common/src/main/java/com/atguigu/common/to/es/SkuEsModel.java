package com.atguigu.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuEsModel {
  private Long skuId;
  private Long spuId;
  private String skuTitle;
  private BigDecimal skuPrice; // 价格
  private String skuImg; // 默认图片
  private Long saleCount; // 销量
  private Boolean hasStock; // 是否有库存
  private Long hotScore; // 热度评分
  private Long brandId; // 品牌id
  private Long catalogId; // 分类id
  private String brandName; // 品牌名称
  private String brandImg; // 品牌图片
  private String catalogName; // 分类名称

  private List<Attrs> attrs; // 商品规格属性信息

  @Data
  public static class Attrs {
    private Long attrId;
    private String attrName;
    private String attrValue;
  }
}
