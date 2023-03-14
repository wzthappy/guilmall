package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.SKuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SkuSaleAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

  PageUtils queryPage(Map<String, Object> params);

  List<SKuItemVo.SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId);

  List<String> getSkuSaleAttrvaluesAsStringList(Long skuId);
}

