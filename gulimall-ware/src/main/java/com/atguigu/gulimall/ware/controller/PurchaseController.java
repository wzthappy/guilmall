package com.atguigu.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;


/**
 * 采购信息
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2023-01-24 13:49:55
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
  @Autowired
  private PurchaseService purchaseService;

  /**
   *  purchase/done   完成采购单
   *  {id: 1,
   *  items: [{itemId: 2,
   *    *         status: 3,
   *    *         reason: ""},{itemId: 6,
   *    *         status: 4,
   *    *         reason: "无货"}]}
   */
  @PostMapping("/done")
  public R done(@RequestBody PurchaseDoneVo doneVo) {
    purchaseService.done(doneVo);
    return R.ok();
  }

  /**
   *  /ware/purchase/received
   *  领取采购单
   */
  @PostMapping("/received")
  public R received(@RequestBody List<Long> ids) {
    purchaseService.received(ids);
    return R.ok();
  }

  // ware/purchase/merge
  @PostMapping("/merge")
  public R merge(@RequestBody MergeVo mergeVo) {
    purchaseService.mergePurchase(mergeVo);

    return R.ok();
  }

  // /unreceive/list
  @RequestMapping("/unreceive/list")
  public R unreceiveList(@RequestParam Map<String, Object> params) {
    PageUtils page = purchaseService.queryPageUnreceive(params);

    return R.ok().put("page", page);
  }

  /**
   * 列表
   */
  @RequestMapping("/list")
  public R list(@RequestParam Map<String, Object> params) {
    PageUtils page = purchaseService.queryPage(params);

    return R.ok().put("page", page);
  }

  /**
   * 信息
   */
  @RequestMapping("/info/{id}")
  public R info(@PathVariable("id") Long id) {
    PurchaseEntity purchase = purchaseService.getById(id);

    return R.ok().put("purchase", purchase);
  }

  /**
   * 保存
   */
  @RequestMapping("/save")
  public R save(@RequestBody PurchaseEntity purchase) {
    purchase.setCreateTime(new Date());
    purchase.setUpdateTime(new Date());
    purchaseService.save(purchase);

    return R.ok();
  }

  /**
   * 修改
   */
  @RequestMapping("/update")
  public R update(@RequestBody PurchaseEntity purchase) {
    purchaseService.updateById(purchase);

    return R.ok();
  }

  /**
   * 删除
   */
  @RequestMapping("/delete")
  public R delete(@RequestBody Long[] ids) {
    purchaseService.removeByIds(Arrays.asList(ids));

    return R.ok();
  }

}
