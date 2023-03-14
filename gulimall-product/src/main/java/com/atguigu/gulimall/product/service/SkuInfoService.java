package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.SKuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * sku信息
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

  PageUtils queryPage(Map<String, Object> params);

  void saveSkuInfo(SkuInfoEntity skuInfoEntity);

  PageUtils queryPageByCondition(Map<String, Object> params);

  List<SkuInfoEntity> getSkusBySpuId(Long spuId);

  SKuItemVo item(Long skuId) throws ExecutionException, InterruptedException;
}

