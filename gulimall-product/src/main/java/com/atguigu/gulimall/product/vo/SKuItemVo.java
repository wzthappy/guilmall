package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SKuItemVo {
  // 1. sku基本信息获取  pms_sku_info
  private SkuInfoEntity info;

  private boolean hasStock = true;

  // 2. sku的图片信息  pms_sku_images
  private List<SkuImagesEntity> images;

  // 3. 获取spu的销售属性组合
  List<SkuItemSaleAttrVo> saleAttr;

  // 4. 获取spu的介绍
  private SpuInfoDescEntity desp;

  // 5. 获取spu的规格参数消息
  private List<SpuItemAttrGroupVo>  groupAttrs;

  // 6. 当前商品的秒杀优惠信息
  private SeckillInfoVo seckillInfo;





  public SeckillInfoVo getSeckillInfo() {
    return seckillInfo;
  }

  public void setSeckillInfo(SeckillInfoVo seckillInfo) {
    this.seckillInfo = seckillInfo;
  }

  @Data
  public static class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
  }

  @Data
  public static class AttrValueWithSkuIdVo {
    private String attrValue;
    private String skuIds;
  }

  @Data
  public static class SpuItemAttrGroupVo {
    private String groupName;
    private List<SpuBaseAttrVO> attrs;
  }

  @Data
  public static class SpuBaseAttrVO {
    private String attrName;
    private String attrValue;
  }
}
