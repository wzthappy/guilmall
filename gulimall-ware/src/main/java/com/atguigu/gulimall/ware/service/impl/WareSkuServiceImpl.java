package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.vo.*;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private WareSkuDao wareSkuDao;

  @Autowired
  private OrderFeignService orderFeignService;

  @Autowired
  private WareOrderTaskService orderTaskService;

  @Autowired
  private WareOrderTaskDetailService orderTaskDetailService;

  @Autowired
  private ProductFeignService productFeignService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    //skuId=1&wareId=1
    QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
    String skuId = (String) params.get("skuId");
    String wareId = (String) params.get("wareId");
    if (!StringUtils.isEmpty(skuId)) {
      queryWrapper.eq("sku_id", skuId);
    }
    if (!StringUtils.isEmpty(wareId)) {
      queryWrapper.eq("ware_id", wareId);
    }


    IPage<WareSkuEntity> page = this.page(
        new Query<WareSkuEntity>().getPage(params),
        queryWrapper
    );

    return new PageUtils(page);
  }

  @Override
  public void addStock(Long skuId, Long wareId, Integer skuNum) {
    // 1. 判断如果还没有这个库存记录 就 新增
    List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>()
        .eq("sku_id", skuId).eq("ware_id", wareId));
    if (entities == null || entities.size() == 0) {
      WareSkuEntity skuEntity = new WareSkuEntity();
      skuEntity.setSkuId(skuId);
      skuEntity.setStock(skuNum);
      skuEntity.setWareId(wareId);
      skuEntity.setStockLocked(0);
      // TODO 远程查询sku名字，如果失败，整个事务无须回滚
      // 1. 自己catch异常
      // TODO 还可以用什么办法让获取名称 出现异常后不回滚？
      try {
        R info = productFeignService.info(skuId);
        Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
        if (info.getCode() == 0) {
          skuEntity.setSkuName((String) data.get("skuName"));
        }
      } catch (Exception e) {
      }
      wareSkuDao.insert(skuEntity);
    } else {
      wareSkuDao.addStock(skuId, wareId, skuNum);
    }
  }

  @Override
  public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
    // 返回对应skuId的商品是否有库存
    List<SkuHasStockVo> vos = baseMapper.getSkuStock(skuIds);
    return vos;
  }


  /**
   * 为某个订单锁定库存
   * <p>
   * 库存解锁的场景:
   * 1)、下订单成功，订单过期没有支付被系统自动取消、被用户手动取消。都要解锁库存
   * 2)、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚
   * 之前锁定的库存就要自动解锁。
   */
  @Override
  // 默认只要是运行时异常都会回滚
  @Transactional(rollbackFor = NoStockException.class) // 表示如果发生NoStockException异常时，《必须》要回滚
  public Boolean orderLockStock(WareSkuLockVo vo) {
    /**
     * 保存库存工作单的详情。
     * 追溯。
     */
    WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
    taskEntity.setOrderSn(vo.getOrderSn());
    orderTaskService.save(taskEntity);

    // 1. 按照下单的收货地址，找到一个就进仓库，锁定库存。

    // 1. 找到每个商品在那个仓库都有库存
    List<OrderItemVO> locks = vo.getLocks();
    List<SkuWareHasStock> collect = locks.stream().map(item -> {
      SkuWareHasStock stock = new SkuWareHasStock();
      Long skuId = item.getSkuId();
      stock.setSkuId(skuId);
      stock.setNum(item.getCount());
      // 查询这个商品在哪里有库存
      List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
      stock.setWareId(wareIds);
      return stock;
    }).collect(Collectors.toList());

    // 2. 锁定库存
    for (SkuWareHasStock hasStock : collect) {
      Boolean skuStocked = false;
      Long skuId = hasStock.getSkuId();
      List<Long> wareIds = hasStock.getWareId();
      if (wareIds == null || wareIds.size() == 0) {
        // 没有任何仓库有这个商品的库存，抛出异常   事务回滚
        throw new NoStockException(skuId);
      }
      // 1. 如果每一个商品都锁定成功，将当前商品锁定了几件的工作单记录发给MQ
      // 2. 锁定失败。前面保存的工作单信息就回滚了，发送出去的消息，即使要解除记录，由于去数据查不到Id，所有就不用解锁

      for (Long wareId : wareIds) {
        // 成功就返回1，否则就是0
        long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
        if (count == 1) {
          skuStocked = true;
          // TODO 告诉MQ库存锁定成功
          WareOrderTaskDetailEntity entity =
              new WareOrderTaskDetailEntity(null, skuId, null,
                  hasStock.getNum(), taskEntity.getId(), wareId, 1);
          orderTaskDetailService.save(entity);

          StockLockedTo lockedTo = new StockLockedTo();
          lockedTo.setId(taskEntity.getId());
          StockDetailTo stockDetailTo = new StockDetailTo();
          BeanUtils.copyProperties(entity, stockDetailTo);
          // 只发Id不行，防止回滚以后找不到数据
          lockedTo.setDetail(stockDetailTo);
          rabbitTemplate.convertAndSend("stock-event-exchange",
              "stock.locked", lockedTo);
          break;
        } else {
          // 当前仓库锁失败，重试下一个仓库

        }
      }
      if (!skuStocked) {
        // 当前商品所有仓库都没有锁住
        throw new NoStockException(skuId);
      }
    }
    // 3. 肯定全部都是锁定成功过的
    return true;
  }

  @Override
  public void unlockStock(StockLockedTo to) {
    StockDetailTo detail = to.getDetail();
    Long detailId = detail.getId();
    // 解锁
    // 1. 查询数据库关于这个订单的锁定库存信息
    //    有: 库存成功，但库存下面的业务失败，也需要回滚
    //              1. 没有这个订单。必须解锁
    //              2. 有这个订单。不是解锁库存
    //                     订单状态: 已取消: 解锁库存
    //                             没取消: 不能解锁
    //   没有: 库库锁定失败了，库存回滚了。这种情况无须解锁
    WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
    if (byId != null) {      // 解锁    库存代码执行成功
      Long id = to.getId();
      WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
      String orderSn = taskEntity.getOrderSn(); // 根据订单号查询订单的状态
      R r = orderFeignService.getOrderStatus(orderSn);
      if (r.getCode() == 0) {
        // 订单数据返回成功
        OrderVo data = r.getData(new TypeReference<OrderVo>() {
        });
        if (data == null || data.getStatus() == 4) {
          // 订单不存在   订单回滚了   但库存执行成功了   ||   订单已经被取消了。才能解锁库存
          if (byId.getLockStatus() == 1) {
            // 当前库存工作单详情，状态1 已锁定但是未解锁才可以解锁
            unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
          }
//          channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
      } else {
        // 消息拒绝以后重新放入队列中，让别人继续消费解锁
        throw new RuntimeException("远程服务失败");
//        channel.basicNack(message.getMessageProperties().getDeliveryTag(), true, false);
      }
    } else {
      // 库存代码执行失败， 库存中有本地事务 会自动回滚，  使用没有这个锁了， 《无须解锁》
//        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
  }

  /**
   * 防止订单服务卡顿，导致订单状态消息一直解锁不了，库存信息优先到期。查询订单状态肯定是新建状态，所以什么都不会做就走了。
   * 导致卡顿的订单，永远不能解锁库存
   */
  @Override
  @Transactional
  public void unlockStock(OrderTo orderTo) {
    String orderSn = orderTo.getOrderSn();
    // 查一下最新的状态，防止重复解锁库存
    WareOrderTaskEntity task = orderTaskService.getOrderTaskByOrderSn(orderSn);
    Long id = task.getId();
    // 按照工作单找到所有  没有解锁的库存，进行解锁
    List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
        .eq("task_id", id).eq("lock_status", 1));
    for (WareOrderTaskDetailEntity entity : entities) {
      unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
    }
  }


  private void unLockStock(Long skuId, Long wareId, Integer num, Long taskId) {
    // 库存解锁
    wareSkuDao.unlockStock(skuId, wareId, num);  //   回退库存
    // 更新库存工作单的状态
    WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
    entity.setId(taskId);
    entity.setLockStatus(2); // 变为以解锁
    orderTaskDetailService.updateById(entity);
  }

  @Data
  class SkuWareHasStock {
    private Long skuId;
    private Integer num;
    private List<Long> wareId;
  }
}