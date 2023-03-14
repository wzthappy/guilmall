package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

  void saveAttr(AttrVo attr);

  PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type);

  AttrRespVo getAttrInfo(Long attrId);

  void updateAttr(AttrVo attr);

  List<AttrEntity> getRelationAttr(Long attrgroupId);

  PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgoupId);

  // 在指定的所有属性集合里面，挑出检索属性
  List<Long> selectSearchAttrs(List<Long> attrIds);
}

