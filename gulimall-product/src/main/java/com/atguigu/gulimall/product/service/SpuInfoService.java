package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

  PageUtils queryPage(Map<String, Object> params);

  void saveSpuInfo(SpuSaveVo vo);

  void saveBaseSpuInfo(SpuInfoEntity infoEntity);

  PageUtils queryPageByCondition(Map<String, Object> params);

  // 商品上架
  void up(Long spuId);

  SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

