package com.atguigu.gulimall.ware.service;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.gulimall.ware.vo.LockStockResult;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-15 15:10:00
 */
public interface WareSkuService extends IService<WareSkuEntity> {

  PageUtils queryPage(Map<String, Object> params);

  void addStock(Long skuId, Long wareId, Integer skuNum);

  List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

  Boolean orderLockStock(WareSkuLockVo vo);

  void unlockStock(StockLockedTo to);

  void unlockStock(OrderTo orderTo);
}

