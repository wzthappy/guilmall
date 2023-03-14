package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 15:23:10
 */
public interface CategoryService extends IService<CategoryEntity> {

  PageUtils queryPage(Map<String, Object> params);

  List<CategoryEntity> listWithTree();

  void removeMenuByIds(List<Long> asList);

  // 找到catelogId的完整路径
  // 【父/子/孙】
  Long[] findCatelogPath(Long catelogId);

  void updateCascade(CategoryEntity category);

  List<CategoryEntity> getLevel1Categorys();

  Map<String, List<Catelog2Vo>> getCatalogJson();
}

