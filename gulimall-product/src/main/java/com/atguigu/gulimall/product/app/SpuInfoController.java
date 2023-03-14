package com.atguigu.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.service.SpuInfoService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;


/**
 * spu信息
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-14 16:34:26
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
  @Autowired
  private SpuInfoService spuInfoService;

  /**
   *  /product/spuinfo/{spuId}/up
   */
  @PostMapping("/{spuId}/up")
  public R spuUp(@PathVariable("spuId") Long spuId) {
    spuInfoService.up(spuId);
    return R.ok();
  }

  @GetMapping("/skuId/{id}")
  public R getSpuInfoBySkuId(@PathVariable("id") Long skuId) {
    SpuInfoEntity entity = spuInfoService.getSpuInfoBySkuId(skuId);
    return R.ok().setData(entity);
  }

  /**
   *  /product/spuinfo/list?t=1672810332662&
   *        status=1 & key=node 6 & brandId=35 & catelogId=225 & page=1 & limit=10
   * 列表
   */
  @RequestMapping("/list")
  public R list(@RequestParam Map<String, Object> params) {
    PageUtils page = spuInfoService.queryPageByCondition(params);

    return R.ok().put("page", page);
  }


  /**
   * 信息
   */
  @RequestMapping("/info/{id}")
  public R info(@PathVariable("id") Long id) {
    SpuInfoEntity spuInfo = spuInfoService.getById(id);

    return R.ok().put("spuInfo", spuInfo);
  }

  /**
   * 保存
   */
  @RequestMapping("/save")
  public R save(@RequestBody SpuSaveVo vo) {
//		spuInfoService.save(spuInfo);
    spuInfoService.saveSpuInfo(vo);

    return R.ok();
  }

  /**
   * 修改
   */
  @RequestMapping("/update")
  public R update(@RequestBody SpuInfoEntity spuInfo) {
    spuInfoService.updateById(spuInfo);

    return R.ok();
  }

  /**
   * 删除
   */
  @RequestMapping("/delete")
  public R delete(@RequestBody Long[] ids) {
    spuInfoService.removeByIds(Arrays.asList(ids));

    return R.ok();
  }

}
