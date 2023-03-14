package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
  private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

  @Autowired
  private ThreadPoolExecutor executor;

  @Autowired  // mq
  private RabbitTemplate rabbitTemplate;

  @Autowired  // redis
  private StringRedisTemplate redisTemplate;

  @Autowired
  private CartFeignService cartFeignService;

  @Autowired
  private MemberFeignService memberFeignService;

  @Autowired
  private OrderItemService orderItemService;

  @Autowired
  private PaymentInfoService paymentInfoService;

  @Autowired
  private WmsFeignService wmsFeignService;



  @Autowired
  private ProductFeignService productFeignService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<OrderEntity> page = this.page(
        new Query<OrderEntity>().getPage(params),
        new QueryWrapper<OrderEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
    MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get(); // 获取当前登录的用户
    OrderConfirmVo confirmVo = new OrderConfirmVo();

    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

    // feign在远程调用之前要构造请求，调用很多的拦截器
    CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
      // 把当前的上下文对象共享到当前多线程中
      RequestContextHolder.setRequestAttributes(requestAttributes);

      // 1. 远程查询所有的收货列表
      List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
      confirmVo.setAddress(address);
    }, executor);

    CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
      // 把当前的上下文对象共享到当前多线程中
      RequestContextHolder.setRequestAttributes(requestAttributes);

      // 2. 远程查询购物车所有选中的购物项
      List<OrderItemVO> cartItems = cartFeignService.getCurrentUserCartItems();
      confirmVo.setItems(cartItems);
    }, executor).thenRunAsync(() -> {
      // 把当前的上下文对象共享到当前多线程中
      RequestContextHolder.setRequestAttributes(requestAttributes);

      List<OrderItemVO> items = confirmVo.getItems();
      List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());

      R hasStock = wmsFeignService.getSkusHasStock(collect);
      if (hasStock != null) {
        List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
        });
        if (data != null) {
          Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
          confirmVo.setStocks(map);
        }
      }
    }, executor);


    // 3. 查询用户积分
    Integer integration = memberRespVo.getIntegration();
    confirmVo.setIntegration(integration);

    // 4. 其他数据自动计算

    // TODO: 5. 防重令牌
    String token = UUID.randomUUID().toString().replaceAll("-", "");
    redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX
        + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);

    confirmVo.setOrderToken(token);

    // 等全部任务做完
    CompletableFuture.allOf(getAddressFuture, getCartFuture).get();

    return confirmVo;
  }

  @Override
  @Transactional // 本地事务，在分布式系统，只能控制住自己的回滚，控制不了其他服务的回滚。
  // 分布式事务: 最大原因。网络问题+分布式机器
//  @GlobalTransactional  // 分布式事务
  public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
    confirmVoThreadLocal.set(vo);
    SubmitOrderResponseVo response = new SubmitOrderResponseVo();
    MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
    response.setCode(0);
    // 1. 验证令牌 【令牌的对比和删除必须保证原子性】
    // 0令牌失败 - 1删除成功
    String sctipt = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    String orderToken = vo.getOrderToken();
    String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX
        + memberRespVo.getId());
    // 原子验证令牌和删除令牌
    Long result = redisTemplate.execute(new DefaultRedisScript<Long>(sctipt, Long.class),
        Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
    if (result == 0L) {
      response.setCode(1);
      // 令牌验证失败
      return response;
    } else {
      // 令牌验证成功
      // 下单: 去创建订单，验令牌，验价格，锁库存...
      // 1. 创建订单，订单项等信息
      OrderCreateTo order = createOrder();
      // 2. 验价
      BigDecimal payAmount = order.getOrder().getPayAmount();
      BigDecimal payPrice = vo.getPayPrice();
      if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
        // 金额对比成功
        // TODO 3. 保存订单
        saveOrder(order);
        // 4. 库存锁定。 只要有异常回滚订单数据。
        // 订单号，所有订单项 (skuId，skuName, num)
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(order.getOrder().getOrderSn());
        List<OrderItemVO> locks = order.getOrderItems().stream().map(item -> {
          OrderItemVO itemVO = new OrderItemVO();
          itemVO.setSkuId(item.getSkuId());
          itemVO.setCount(item.getSkuQuantity());
          itemVO.setTitle(item.getSkuName());
          return itemVO;
        }).collect(Collectors.toList());
        lockVo.setLocks(locks);
        // TODO 4. 远程锁库存
        // 库存成功了，当是网络原因超时了，订单回滚，库存不滚
        R r = wmsFeignService.orderLockStock(lockVo);
        if (r.getCode() == 0) {
          // 锁成功了
          response.setOrder(order.getOrder());
          // TODO 5. 远程扣减积分
//          int i = 10 / 0;
          // TODO  订单创建成功给MQ
          rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order",
              order.getOrder());
          return response;
        } else {
          // 锁定失败
          throw new NoStockException(order.getOrder().getId());
//          response.setCode(3);
//          return response;
        }
      } else {
        response.setCode(2); // 金额对比失败
        return response;
      }
    }
    //if (orderToken != null && orderToken.equals(redisToken)) {// 令牌验证通过因为有可能删除的是后一个的UUID所以使用lua脚本Boolean delete = redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX     + memberRespVo.getId());} else {不通过}
  }

  @Override
  public OrderEntity getOrderByOrderSn(String orderSn) {
    OrderEntity one = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    return one;
  }

  @Override
  public void closeOrder(OrderEntity entity) {
    // 查询当前这个点单的最新状态
    OrderEntity orderEntity = this.getById(entity.getId());
    if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
      // 关单
      OrderEntity update = new OrderEntity();
      update.setId(entity.getId());
      update.setStatus(OrderStatusEnum.CANCLED.getCode());
      this.updateById(update);
      OrderTo orderTo = new OrderTo();
      BeanUtils.copyProperties(orderEntity, orderTo);
      // 发给MQ一个
      try {
        // TODO: 保证消息一定能够发出去，每一个消息都可以做好日志记录(给数据库保存每一个消息的详细信息)
        // TODO: 定期扫描数据库将失败的消息在在发送一遍
        rabbitTemplate.convertAndSend("order-event-exchange",
            "order.release.other", orderTo);
      } catch (Exception e) {
        // TODO: 将没有发送成功的消息进行重试发送

      }
    }
  }

  @Override
  public PayVo getOrderPay(String orderSn) {
    PayVo payVo = new PayVo();
    OrderEntity order = this.getOrderByOrderSn(orderSn);
    List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
    OrderItemEntity entity = order_sn.get(0);
    BigDecimal bigDecimal = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);

    payVo.setOut_trade_no(order.getOrderSn()); // 订单号
    payVo.setBody(entity.getSkuAttrsVals()); // 订单的备注
    payVo.setSubject(entity.getSkuName()); // 订单的主题(名称)
    payVo.setTotal_amount(bigDecimal.toString()); // 订单的金额

    return payVo;
  }

  @Override
  public PageUtils queryPageWithItem(Map<String, Object> params) {
    MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

    IPage<OrderEntity> page = this.page(
        new Query<OrderEntity>().getPage(params),
        new QueryWrapper<OrderEntity>()
            .eq("member_id", memberRespVo.getId())
            .orderByDesc("id")
    );
    List<OrderEntity> order_sn = page.getRecords().stream().map(order -> {
      List<OrderItemEntity> itemEntities = orderItemService.list(
          new QueryWrapper<OrderItemEntity>()
          .eq("order_sn", order.getOrderSn()));
      order.setItemEntities(itemEntities);
      return order;
    }).collect(Collectors.toList());
    page.setRecords(order_sn);
    return new PageUtils(page);
  }

  /**
   * 处理支付宝的支付结果
   * @param vo
   * @return
   */
  @Override
  public String handlePayResult(PayAsyncVo vo) {
    // 1. 保存交易流水
    PaymentInfoEntity infoEntity = new PaymentInfoEntity();
    infoEntity.setAlipayTradeNo(vo.getTrade_no());
    infoEntity.setOrderSn(vo.getOut_trade_no());
    infoEntity.setPaymentStatus(vo.getTrade_status());
    infoEntity.setCallbackTime(vo.getNotify_time());
    String total_amount = vo.getTotal_amount();
    infoEntity.setTotalAmount(new BigDecimal(total_amount));

    paymentInfoService.save(infoEntity);

    // 2. 修改订单的状态消息
    if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
      // 支付成功状态
      String outTradeNo = vo.getOut_trade_no();
      this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
    }
    return "success";
  }

  /**
   * 保存秒杀的订单
   * @param seckillOrder
   */
  @Override
  public void createSeckillOrder(SeckillOrderTo seckillOrder) {
    // TODO 保存订单消息
    OrderEntity orderEntity = new OrderEntity();
    orderEntity.setOrderSn(seckillOrder.getOrderSn());
    orderEntity.setMemberId(seckillOrder.getMemberId());
    orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

    BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
    orderEntity.setPayAmount(multiply);
    this.save(orderEntity);

    // TODO 保存订单项信息
    OrderItemEntity orderItemEntity = new OrderItemEntity();
    orderItemEntity.setOrderSn(seckillOrder.getOrderSn());
    orderItemEntity.setRealAmount(multiply);
    // TODO 获取当前SKU的详细信息进行设置
//    productFeignService.getSpuInfoBySkuId()
    orderItemEntity.setSkuQuantity(seckillOrder.getNum());

    orderItemService.save(orderItemEntity);
  }

  /**
   * 保存订单数据
   */
  private void saveOrder(OrderCreateTo order) {
    OrderEntity orderEntity = order.getOrder();
    orderEntity.setModifyTime(new Date());
    this.save(orderEntity);

    List<OrderItemEntity> orderItems = order.getOrderItems();
    orderItemService.saveBatch(orderItems);
  }

  private OrderCreateTo createOrder() {
    OrderCreateTo createTo = new OrderCreateTo();
    // 1. 生成订单号
    String orderSn = IdWorker.getTimeId();
    // 创建订单
    OrderEntity orderEntity = buildOrder(orderSn);
    createTo.setOrder(orderEntity);

    // 2. 获取到所有的订单项
    List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
    createTo.setOrderItems(itemEntities);

    // 3. 计算价格、积分等相关信息
    computePrice(orderEntity, itemEntities);

    return createTo;
  }

  private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
    BigDecimal total = new BigDecimal("0.0");
    BigDecimal coupon = new BigDecimal("0.0");
    BigDecimal integration = new BigDecimal("0.0");
    BigDecimal promotion = new BigDecimal("0.0");

    Integer gift = 0;
    Integer growth = 0;
    // 订单的总额，叠加每一个订单项的总额信息
    for (OrderItemEntity entity : itemEntities) {
      coupon = coupon.add(entity.getCouponAmount());
      integration = integration.add(entity.getIntegrationAmount());
      promotion = promotion.add(entity.getPromotionAmount());
      total = total.add(entity.getRealAmount());
      gift += entity.getGiftIntegration();
      growth += entity.getGiftGrowth();
    }
    // 1. 订单价格相关
    orderEntity.setTotalAmount(total);
    // 应付总额
    orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

    orderEntity.setPromotionAmount(promotion);
    orderEntity.setIntegrationAmount(integration);
    orderEntity.setCouponAmount(coupon);

    // 设置积分等信息
    orderEntity.setIntegration(gift);
    orderEntity.setGrowth(growth);
    orderEntity.setDeleteStatus(0); // 订单未删除 0     1(删除)
  }

  private OrderEntity buildOrder(String orderId) {
    MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
    OrderEntity entity = new OrderEntity();
    entity.setOrderSn(orderId);
    entity.setMemberId(respVo.getId());

    // 获取收获地址
    OrderSubmitVo submitVo = confirmVoThreadLocal.get();

    R fare = wmsFeignService.getFare(submitVo.getAddrId());
    FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
    });

    // 设置运费信息
    entity.setFreightAmount(fareResp.getFare());
    // 设置收人信息
    entity.setReceiverCity(fareResp.getAddress().getCity());
    entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
    entity.setReceiverName(fareResp.getAddress().getName());
    entity.setReceiverPhone(fareResp.getAddress().getPhone());
    entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
    entity.setReceiverProvince(fareResp.getAddress().getProvince());
    entity.setReceiverRegion(fareResp.getAddress().getRegion());

    // 设置订单的相关状态信息
    entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
    entity.setAutoConfirmDay(7); // 自动确认时间7天


    return entity;
  }

  /**
   * 构建所有订单项数据
   */
  private List<OrderItemEntity> buildOrderItems(String orderSn) {
    // 最后确定购物项的价格
    List<OrderItemVO> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
    if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
      List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
        OrderItemEntity itemEntity = buildOrderItem(cartItem);
        itemEntity.setOrderSn(orderSn);
        return itemEntity;
      }).collect(Collectors.toList());
      return itemEntities;
    }
    return null;
  }

  /**
   * 构建某一个订单项
   */
  private OrderItemEntity buildOrderItem(OrderItemVO cartItem) {
    OrderItemEntity itemEntity = new OrderItemEntity();
    // 1. 订单信息: 订单号
    // 2. 商品的SPU信息
    Long skuId = cartItem.getSkuId();
    R r = productFeignService.getSpuInfoBySkuId(skuId);
    SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
    });
    itemEntity.setSpuId(data.getId());
    itemEntity.setSpuBrand(data.getBrandId().toString());
    itemEntity.setSpuName(data.getSpuName());
    itemEntity.setCategoryId(data.getCatalogId());

    // 3. 商品的sku信息
    itemEntity.setSkuId(cartItem.getSkuId());
    itemEntity.setSkuName(cartItem.getTitle());
    itemEntity.setSkuPic(cartItem.getImage());
    itemEntity.setSkuPrice(cartItem.getPrice());
    String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
    itemEntity.setSkuAttrsVals(skuAttr);
    itemEntity.setSkuQuantity(cartItem.getCount());

    // 4. 优惠信息[不做]
    // 5. 积分信息
    itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
    itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

    // 6. 订单项的价格信息
    itemEntity.setPromotionAmount(new BigDecimal("0"));
    itemEntity.setCouponAmount(new BigDecimal("0"));
    itemEntity.setIntegrationAmount(new BigDecimal("0"));
    // 当前订单项的实际金额    总额-各种优惠
    BigDecimal orign = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
    BigDecimal subtract = orign.subtract(itemEntity.getCouponAmount())
        .subtract(itemEntity.getPromotionAmount())
        .subtract(itemEntity.getIntegrationAmount());
    itemEntity.setRealAmount(subtract);

    return itemEntity;
  }
}