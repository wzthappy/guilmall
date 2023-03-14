package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.baomidou.mybatisplus.extension.service.additional.query.impl.LambdaQueryChainWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {
  @Autowired
  private CategoryBrandRelationService categoryBrandRelationService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    QueryWrapper<BrandEntity> queryWrapper = new QueryWrapper<>();
    // 1. 获取key
    String key = (String) params.get("key");
    if (!StringUtils.isEmpty(key)) {
      queryWrapper.eq("brand_id", key).or().like("name", key);
    }

    IPage<BrandEntity> page = this.page(
        new Query<BrandEntity>().getPage(params),
        queryWrapper
    );

    return new PageUtils(page);
  }

  @Override
  @Transactional // 事务
  public void updateDeta(BrandEntity brand) {
    // 保证冗余字段的数据一致
    this.updateById(brand);
    if (!StringUtils.isEmpty(brand.getName())) {
      // 同步更新其他关联表中的数据
      categoryBrandRelationService.updateBrand(brand.getBrandId(), brand.getName());

      // TODO 更新其他关联

    }
  }

  @Override
  public List<BrandEntity> getBrandsByIds(List<Long> brandIds) {
    return baseMapper.selectList(new QueryWrapper<BrandEntity>().in("brand_id", brandIds));
  }

}