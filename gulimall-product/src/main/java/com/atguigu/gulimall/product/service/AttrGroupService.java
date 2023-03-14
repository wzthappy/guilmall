package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.atguigu.gulimall.product.vo.SKuItemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

  PageUtils queryPage(Map<String, Object> params);

  PageUtils queryPage(Map<String, Object> params, Long catelogId);

  void deleteRelation(AttrGroupRelationVo[] vos);

  List<AttrGroupWithAttrsVo> getAttrGroupWxithAttrsByCatelogId(Long catelogId);

  List<SKuItemVo.SpuItemAttrGroupVo> getAttrGroupWxithAttrsBySpuId(Long spuId, Long catalogId);
}

