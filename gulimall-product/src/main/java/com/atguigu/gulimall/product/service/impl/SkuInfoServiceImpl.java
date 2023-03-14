package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.SKuItemVo;
import com.atguigu.gulimall.product.vo.SeckillInfoVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
  @Autowired
  private SeckillFeignService seckillFeignService;

  @Autowired
  private SkuImagesService imageService;

  @Autowired
  private SpuInfoDescService spuInfoDescService;

  @Autowired
  private AttrGroupService attrGroupService;

  @Autowired
  private SkuSaleAttrValueService skuSaleAttrValueService;

  @Autowired
  private ThreadPoolExecutor executor;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<SkuInfoEntity> page = this.page(
        new Query<SkuInfoEntity>().getPage(params),
        new QueryWrapper<SkuInfoEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
    this.baseMapper.insert(skuInfoEntity);
  }

  @Override
  public PageUtils queryPageByCondition(Map<String, Object> params) {
    QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
    /**
     *  page=1&limit=10&
     *      key=华为&catelogId=225&brandId=35&min=6000&max=7000
     */
    String key = (String) params.get("key");
    if (!StringUtils.isEmpty(key)) {
      wrapper.and(w -> {
        w.eq("sku_id", key).or().like("sku_name", key);
      });
    }
    String catelogId = (String) params.get("catelogId");
    if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
      wrapper.eq("catalog_id", catelogId);
    }
    String brandId = (String) params.get("brandId");
    if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
      wrapper.eq("brand_id", brandId);
    }
    String min = (String) params.get("min");
    if (!StringUtils.isEmpty(min)) {
      wrapper.ge("price", min);
    }
    String max = (String) params.get("max");
    if (!StringUtils.isEmpty(max)) {
      try {
        BigDecimal bigDecimal = new BigDecimal(max);
        if (bigDecimal.compareTo(new BigDecimal("0")) == 1) {
          wrapper.le("price", max);
        }
      } catch (Exception e) {

      }
    }
    IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), wrapper);

    return new PageUtils(page);
  }

  @Override
  public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
    List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>()
        .eq("spu_id", spuId));
    return list;
  }

  @Override
  public SKuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
    SKuItemVo sKuItemVo = new SKuItemVo();

    CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
      // 1. sku基本信息获取  pms_sku_info
      SkuInfoEntity info = this.getById(skuId);
      sKuItemVo.setInfo(info);
      return info;
    }, executor);

    CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
      // 3. 获取spu的销售属性组合
      List<SKuItemVo.SkuItemSaleAttrVo> saleAttrVo = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
      sKuItemVo.setSaleAttr(saleAttrVo);
    }, executor);

    CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
      // 4. 获取spu的介绍   pms_sku_info_desc
      SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(res.getSpuId());
      sKuItemVo.setDesp(spuInfoDescEntity);
    }, executor);

    CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
      // 5. 获取spu的规格参数消息
      List<SKuItemVo.SpuItemAttrGroupVo> attrGroupVos =
          attrGroupService.getAttrGroupWxithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
      sKuItemVo.setGroupAttrs(attrGroupVos);
    }, executor);


    CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
      // 2. sku的图片信息  pms_sku_images
      List<SkuImagesEntity> images = imageService.getImagesBySkuId(skuId);
      sKuItemVo.setImages(images);
    }, executor);

    // 3. 查询当前sku是否参与秒杀优惠
    CompletableFuture<Void> secKillFuture = CompletableFuture.runAsync(() -> {
      R seckillInfo = seckillFeignService.getSkuSeckillInfo(skuId);
      if (seckillInfo.getCode() == 0) {
        SeckillInfoVo seckillInfoVo = seckillInfo.getData(new TypeReference<SeckillInfoVo>() {
        });
        sKuItemVo.setSeckillInfo(seckillInfoVo);
      }
    }, executor);


    // 等待所有任务都完成
    CompletableFuture.allOf(saleAttrFuture, descFuture, baseAttrFuture, imageFuture, secKillFuture)
        .get();
    return sKuItemVo;
  }

}