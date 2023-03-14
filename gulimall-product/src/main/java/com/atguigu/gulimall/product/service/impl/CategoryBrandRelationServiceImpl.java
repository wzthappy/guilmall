package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryBrandRelationDao;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

  @Autowired
  private BrandDao brandDao;

  @Autowired
  private CategoryDao categoryDao;

  @Autowired
  private BrandService brandService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<CategoryBrandRelationEntity> page = this.page(
        new Query<CategoryBrandRelationEntity>().getPage(params),
        new QueryWrapper<CategoryBrandRelationEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
    Long brandId = categoryBrandRelation.getBrandId();
    Long catelogId = categoryBrandRelation.getCatelogId();
    // 1. 查询详细名字
    BrandEntity brandEntity = brandDao.selectById(brandId);
    CategoryEntity categoryEntity = categoryDao.selectById(catelogId);

    categoryBrandRelation.setBrandName(brandEntity.getName());
    categoryBrandRelation.setCatelogName(categoryEntity.getName());

    this.save(categoryBrandRelation);
  }

  @Override
  public void updateBrand(Long brandId, String name) {
    CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
    relationEntity.setBrandId(brandId);
    relationEntity.setBrandName(name);
    this.update(relationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
  }

  @Override
  public void updateCategory(Long catId, String name) {
    this.baseMapper.updateCategory(catId, name);
  }

  @Override
  public List<BrandEntity> getBrandsByCatId(Long catId) {
    List<CategoryBrandRelationEntity> catelogId = this.baseMapper.selectList(
        new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));

    List<Long> brandIds = catelogId.stream().map(item -> {
      return item.getBrandId();
    }).collect(Collectors.toList());

    List<BrandEntity> brandEntities = brandService
        .list(new QueryWrapper<BrandEntity>().in("brand_id", brandIds));

    return brandEntities;
  }
}