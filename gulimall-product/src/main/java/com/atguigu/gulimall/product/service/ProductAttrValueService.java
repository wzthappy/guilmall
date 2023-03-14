package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * spu属性值
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

  PageUtils queryPage(Map<String, Object> params);

  void saveProductAttr(List<ProductAttrValueEntity> collect);

  List<ProductAttrValueEntity> baseAttrListforspu(Long spuId);

  void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}

