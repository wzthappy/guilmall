package com.atguigu.gulimall.product.app;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;


/**
 * sku信息
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 16:34:26
 */
@RestController
@RequestMapping("product/skuinfo")
public class SkuInfoController {
  @Autowired
  private SkuInfoService skuInfoService;

  @PostMapping("/getSkuIdAndPrice")
  public Map<Long, BigDecimal> getSkuIdAndPrice(@RequestBody List<Long> listIds) {
    HashMap<Long, BigDecimal> map = new HashMap<>();
    List<SkuInfoEntity> skuInfoEntities = skuInfoService.
        list(new QueryWrapper<SkuInfoEntity>().in("sku_id", listIds));
    skuInfoEntities.forEach(item -> {
      map.put(item.getSkuId(), item.getPrice());
    });
    return map;
  }

  @GetMapping("/{skuId}/price")
  public BigDecimal getPrice(@PathVariable("skuId") Long skuId) {
    return skuInfoService.getById(skuId).getPrice();
  }

  /**
   * /product/skuinfo/list?t=1672815333250&
   * page=1&limit=10&key=华为&catelogId=225&brandId=35&min=6000&max=7000
   * 列表
   */
  @RequestMapping("/list")
  public R list(@RequestParam Map<String, Object> params) {
    PageUtils page = skuInfoService.queryPageByCondition(params);

    return R.ok().put("page", page);
  }


  /**
   * 信息
   */
  @RequestMapping("/info/{skuId}")
  public R info(@PathVariable("skuId") Long skuId) {
    SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
    return R.ok().put("skuInfo", skuInfo);
  }

  /**
   * 保存
   */
  @RequestMapping("/save")
  public R save(@RequestBody SkuInfoEntity skuInfo) {
    skuInfoService.save(skuInfo);

    return R.ok();
  }

  /**
   * 修改
   */
  @RequestMapping("/update")
  public R update(@RequestBody SkuInfoEntity skuInfo) {
    skuInfoService.updateById(skuInfo);

    return R.ok();
  }

  /**
   * 删除
   */
  @RequestMapping("/delete")
  public R delete(@RequestBody Long[] skuIds) {
    skuInfoService.removeByIds(Arrays.asList(skuIds));

    return R.ok();
  }

}
