package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-15 15:10:00
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

  void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

  List<SkuHasStockVo> getSkuStock(@Param("skuIds") List<Long> skuIds);

  List<Long> listWareIdHasSkuStock(@Param("skuId") Long skuId);

  long lockSkuStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);

  void unlockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);
}
