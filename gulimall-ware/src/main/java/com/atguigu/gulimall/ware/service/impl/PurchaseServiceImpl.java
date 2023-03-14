package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

  @Autowired
  private PurchaseDetailService detailService;

  @Autowired
  private WareSkuService wareSkuService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<PurchaseEntity> page = this.page(
        new Query<PurchaseEntity>().getPage(params),
        new QueryWrapper<PurchaseEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public PageUtils queryPageUnreceive(Map<String, Object> params) {
    QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
    // 获取  0表示这个采购单刚新建，1表示这个采购单未分配
    queryWrapper.eq("status", 0).or().eq("status", 1);

    IPage<PurchaseEntity> page = this.page(
        new Query<PurchaseEntity>().getPage(params),
        queryWrapper
    );

    return new PageUtils(page);
  }

  @Override
  @Transactional
  public void mergePurchase(MergeVo mergeVo) {
    Long purchaseId = mergeVo.getPurchaseId();
    List<Long> items = mergeVo.getItems();
    if (purchaseId == null) { // 没有采购单，要自己创建一个新的采购单
      // 1. 新建一个
      PurchaseEntity purchaseEntity = new PurchaseEntity();

      purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
      purchaseEntity.setCreateTime(new Date());
      purchaseEntity.setUpdateTime(new Date());
      this.save(purchaseEntity);
      purchaseId = purchaseEntity.getId();
    }

    // 确认采购单状态   不是正在采购中 就可以了
    PurchaseEntity entity = this.getById(purchaseId);
    if (entity.getStatus() == 2) {
      return;
    }
    AtomicBoolean bStatus = new AtomicBoolean(true);
    Long finalPurchaseId = purchaseId;
    List<PurchaseDetailEntity> collect = items.stream().map(i -> {
      // 确认采购项  2 是正在采购
      PurchaseDetailEntity byId = detailService.getById(i);
      if (byId.getStatus() == 2) {
        bStatus.set(false);
      }

      PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
      detailEntity.setId(i);
      detailEntity.setPurchaseId(finalPurchaseId);
      detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());

      return detailEntity;
    }).collect(Collectors.toList());

    if (bStatus.get()) {
      detailService.updateBatchById(collect);
      // 更新采购单的最新时间
      PurchaseEntity purchaseEntity = new PurchaseEntity();
      purchaseEntity.setId(purchaseId);
      purchaseEntity.setUpdateTime(new Date());
      this.updateById(purchaseEntity);
    }
  }

  /**
   * @param ids 采购单id
   */
  @Override
  public void received(List<Long> ids) {
    // 1. 确认当前采购单是新建或者已分配状态
    List<PurchaseEntity> collect = ids.stream().map(id -> {
      PurchaseEntity byId = this.getById(id);
      return byId;
    }).filter(item -> {
      return item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
          item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
    }).map(item -> {
      item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
      item.setUpdateTime(new Date());
      return item;
    }).collect(Collectors.toList());

    // 2. 改变采购单的状态
    this.updateBatchById(collect);

    // 3. 改变采购项的状态
    collect.forEach(item -> {
      List<PurchaseDetailEntity> entities = detailService.listDetailByPurchase(item.getId());
      List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
        PurchaseDetailEntity entity1 = new PurchaseDetailEntity();
        entity1.setId(entity.getId());
        entity1.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
        return entity1;
      }).collect(Collectors.toList());
      detailService.updateBatchById(detailEntities);
    });
  }

  @Override
  @Transactional
  public void done(PurchaseDoneVo doneVo) {
    Long id = doneVo.getId();

    // 2. 改变采购项的状态
    Boolean flag = true;
    List<PurchaseItemDoneVo> items = doneVo.getItems();

    List<PurchaseDetailEntity> updates = new ArrayList<>();
    for (PurchaseItemDoneVo item : items) {
      PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
      if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
        flag = false;
        detailEntity.setStatus(item.getStatus());
      } else {
        detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
        // 3. 将成功采购的进行入库
        PurchaseDetailEntity entity = detailService.getById(item.getItemId());
        wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());
      }
      detailEntity.setId(item.getItemId());
      updates.add(detailEntity);
    }
    detailService.updateBatchById(updates);

    // 1. 改变采购单状态
    PurchaseEntity purchaseEntity = new PurchaseEntity();
    purchaseEntity.setId(id);
    purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() :
        WareConstant.PurchaseStatusEnum.HASERROR.getCode());
    purchaseEntity.setUpdateTime(new Date());
    this.updateById(purchaseEntity);
  }
}