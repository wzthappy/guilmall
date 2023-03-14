package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.vo.SKuItemVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

  List<SKuItemVo.SpuItemAttrGroupVo> getAttrGroupWxithAttrsBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
