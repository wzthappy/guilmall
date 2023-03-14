package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.ProductConstant;
import com.atguigu.common.to.SkuReductionTo;
import com.atguigu.common.to.SpuBoundTo;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.*;
import com.atguigu.gulimall.product.feign.CouponFeignService;
import com.atguigu.gulimall.product.feign.SearchFeignService;
import com.atguigu.gulimall.product.feign.WareFeignService;
import com.atguigu.gulimall.product.service.*;
import com.atguigu.gulimall.product.vo.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.crypto.Data;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

  @Autowired
  private SpuInfoDescService spuInfoDescService;

  @Autowired
  private SpuImagesService imagesService;

  @Autowired
  private AttrService attrService;

  @Autowired
  private ProductAttrValueService attrValueService;

  @Autowired
  private SkuInfoService skuInfoService;

  @Autowired
  private SkuImagesService skuImagesService;

  @Autowired
  private SkuSaleAttrValueService skuSaleAttrValueService;

  @Autowired
  private CouponFeignService couponFeignService;

  @Autowired
  private BrandService brandService;

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private WareFeignService wareFeignService;

  @Autowired
  private SearchFeignService searchFeignService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<SpuInfoEntity> page = this.page(
        new Query<SpuInfoEntity>().getPage(params),
        new QueryWrapper<SpuInfoEntity>()
    );

    return new PageUtils(page);
  }

  /**
   * // TODO 高级部分完善
   *
   * @param vo
   */
  @Override
  @Transactional
  public void saveSpuInfo(SpuSaveVo vo) {
    // 1. 保存spu基本信息   pms_spu_info
    SpuInfoEntity infoEntity = new SpuInfoEntity();
    BeanUtils.copyProperties(vo, infoEntity);
    infoEntity.setCreateTime(new Date());
    infoEntity.setUpdateTime(new Date());
    this.saveBaseSpuInfo(infoEntity);

    // 2. 保存spu的描述图片  pms_spu_info_desc
    List<String> decript = vo.getDecript();
    SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
    descEntity.setSpuId(infoEntity.getId());
    if (decript != null && decript.size() > 0) {
      descEntity.setDecript(String.join(",", decript));
    }
    spuInfoDescService.saveSpuInfoDesc(descEntity);

    // 3. 保存spu的图片集  pms_spu_images
    List<String> images = vo.getImages();
    imagesService.saveImages(infoEntity.getId(), images);

    // 4. 保存spu的规格参数  pms_product_attr_value
    List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
    List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
      ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
      valueEntity.setAttrId(attr.getAttrId());
      AttrEntity id = attrService.getById(attr.getAttrId());
      valueEntity.setAttrName(id.getAttrName());
      valueEntity.setAttrValue(attr.getAttrValues());
      valueEntity.setQuickShow(attr.getShowDesc());
      valueEntity.setSpuId(infoEntity.getId());

      return valueEntity;
    }).collect(Collectors.toList());
    attrValueService.saveProductAttr(collect);

    // 5. 保存spu的积分信息  gulimall_sms -> sms_spu_bounds
    Bounds bounds = vo.getBounds();
    SpuBoundTo spuBoundTo = new SpuBoundTo();
    BeanUtils.copyProperties(bounds, spuBoundTo);
    spuBoundTo.setSpuId(infoEntity.getId());
    R r = couponFeignService.saveSpuBounds(spuBoundTo);
    if (r.getCode() != 0) {
      log.error("远程保存spu积分信息失败");
    }

    // 6. 保存当前spu对应的所有sku信息
    List<Skus> skus = vo.getSkus();
    if (skus != null && skus.size() > 0) {
      skus.forEach(item -> {
        String defaultImg = "";
        for (Images image : item.getImages()) {
          if (image.getDefaultImg() == 1) {
            defaultImg = image.getImgUrl();
          }
        }

        SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
        BeanUtils.copyProperties(item, skuInfoEntity);
        skuInfoEntity.setBrandId(infoEntity.getBrandId());
        skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
        skuInfoEntity.setSaleCount(0L);
        skuInfoEntity.setSpuId(infoEntity.getId());
        skuInfoEntity.setSkuDefaultImg(defaultImg);
        // 6.1)、sku的基本信息  pms_sku_info
        skuInfoService.saveSkuInfo(skuInfoEntity);

        Long skuId = skuInfoEntity.getSkuId();

        List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
          SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
          skuImagesEntity.setSkuId(skuId);
          skuImagesEntity.setImgUrl(img.getImgUrl());
          skuImagesEntity.setDefaultImg(img.getDefaultImg());
          return skuImagesEntity;
        }).filter(entity -> {
          // 返回true就是需要，false就是剔除
          return !StringUtils.isEmpty(entity.getImgUrl());
        }).collect(Collectors.toList());

        // 6.2)、sku的图片   pms_sku_images
        skuImagesService.saveBatch(imagesEntities);

        // 6.3)、sku的销售属性信息   pms_sku_sale_attr_value
        List<Attr> attr = item.getAttr();
        List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
          SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
          BeanUtils.copyProperties(a, attrValueEntity);
          attrValueEntity.setSkuId(skuId);
          return attrValueEntity;
        }).collect(Collectors.toList());
        skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

        // 6.4)、sku的优惠、满减等信息   gulimall_sms -> sms_sku_ladder \ sms_sku_full_reduction \ sms_member_price
        SkuReductionTo skuReductionTo = new SkuReductionTo();
        BeanUtils.copyProperties(item, skuReductionTo);
        skuReductionTo.setSkuId(skuId);
        if (skuReductionTo.getFullCount() >= 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal(0)) == 1) {
          R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
          if (r1.getCode() != 0) {
            log.error("远程保存sku优惠信息失败");
          }
        }
      });
    }
  }


  @Override
  public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
    this.baseMapper.insert(infoEntity);
  }

  @Override
  public PageUtils queryPageByCondition(Map<String, Object> params) {
    QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

    /**
     *  status=1 & key=node 6 & brandId=35 & catelogId=225 & page=1 & limit=10
     */
    String key = (String) params.get("key");
    if (!StringUtils.isEmpty(key)) {
      wrapper.and(w -> {
        w.eq("id", key).or().like("spu_name", key);
      });
    }
    String status = (String) params.get("status");
    if (!StringUtils.isEmpty(status)) {
      wrapper.eq("publish_status", status);
    }
    String brandId = (String) params.get("brandId");
    if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
      wrapper.eq("brand_id", brandId);
    }
    String catelogId = (String) params.get("catelogId");
    if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
      wrapper.eq("catalog_id", catelogId);
    }

    IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), wrapper);

    return new PageUtils(page);
  }

  @Override
  public void up(Long spuId) {
    // 1. 查出当前spuid对应的所有sku信息，品牌的名称
    List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
    List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

    // TODO 4. 查询当前sku的所有可以被用来检索的规格属性，
    List<ProductAttrValueEntity> baseAttrs = attrValueService.baseAttrListforspu(spuId);

    List<Long> attrIds = baseAttrs.stream().map(attr -> {
      return attr.getAttrId();
    }).collect(Collectors.toList());

    List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);

    Set<Long> idSet = new HashSet<>(searchAttrIds);

    List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item -> {
      return idSet.contains(item.getAttrId());
    }).map(item -> {
      SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
      BeanUtils.copyProperties(item, attrs1);
      return attrs1;
    }).collect(Collectors.toList());

    // TODO 1. 发送远程调用，库存系统查询是否有库存
    Map<Long, Boolean> stockMap = null;  // hasStock
    try {
      R skuHasStock = wareFeignService.getSkusHasStock(skuIdList);
      TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {
      };
      stockMap = skuHasStock.getData(typeReference).stream()
          .collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
    } catch (Exception e) {
      log.error("库存服务查询异常: 原因{}", e);
    }

    // 2. 封装每个sku的信息
    Map<Long, Boolean> finalStockMap = stockMap;
    List<SkuEsModel> uoProducts = skus.stream().map(sku -> {
      // 组装需要的数据
      SkuEsModel esModel = new SkuEsModel();
      BeanUtils.copyProperties(sku, esModel);
      esModel.setSkuPrice(sku.getPrice());
      esModel.setSkuImg(sku.getSkuDefaultImg());
      esModel.setHasStock(false);

      //设置库存信息
      if (finalStockMap == null) {
        esModel.setHasStock(true);
      } else {
        esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
      }

      // TODO 2. 热度评分。   这个可以根据合作商店价格来决定他的热度
      esModel.setHotScore(0L);

      // TODO 3. 查询品牌和分类的名称信息
      BrandEntity brand = brandService.getById(esModel.getBrandId());
      CategoryEntity category = categoryService.getById(esModel.getCatalogId());
      esModel.setBrandName(brand.getName());
      esModel.setBrandImg(brand.getLogo());
      esModel.setCatalogName(category.getName());
      // 设置检索属性
      esModel.setAttrs(attrsList);
      return esModel;
    }).collect(Collectors.toList());

    // TODO 5. 将数据发送给es进行保存。  gulimall-search
    R r = searchFeignService.productStatusUp(uoProducts);
    if (r.getCode() == 0) {
      // 远程调用成功
      // TODO 6. 修改当前spu的状态
      baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
    } else {
      // 远程调用失败
      // TODO 7. 重复调用(接口幂等性)?  重试机制?
      // Feign 调用流程
      /**
       * 1. 构造请求数据，将对象转为json
       *        RequestTemplate template = buildTemplateFromArgs.create(argv);
       * 2. 发送请求进行执行 (执行成功会解码响应数据)
       *        executeAndDecode(template)
       * 3. 执行请求会有重试机制
       *        while (true) {
       *          try {
       *            executeAndDecode(template)
       *          } catch () {
       *            try {retryer.continueOrPropagate(e);} catch () {throw ex;}
       *            continue;
       *          }
       *        }
       */
    }
  }

  @Override
  public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
    SkuInfoEntity byId = skuInfoService.getById(skuId);
    Long spuId = byId.getSpuId();
    SpuInfoEntity spuInfoEntity = this.getById(spuId);
    return spuInfoEntity;
  }
}