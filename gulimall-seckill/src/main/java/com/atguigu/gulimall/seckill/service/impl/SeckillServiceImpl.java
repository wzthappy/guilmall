package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {
  private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
  private final String SKUKILL_CACHE_PREFIX = "seckill:skus";
  private final String SkU_STOCK_SEMAPHORE = "seckill:stock:"; // + 商品随机码

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Autowired
  private RedissonClient redissonClient;

  @Autowired
  private CouponFeignService couponFeignService;

  @Autowired
  private ProductFeignService productFeignService;


  @Override
  public void uplocadSeckSeckillSkuLatest3Days() {
    // 1. 扫描最近三天需要参与秒杀的活动
    R session = couponFeignService.getLate3DaySession();
    if (session.getCode() == 0) {
      // 上架的商品信息
      List<SeckillSessionsWithSkus> sessionData = session
          .getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
          });
      // 缓存到redis中
      // 1. 缓存活动信息
      savaSessionInfos(sessionData);
      // 2. 缓存活动的关联商品信息
      saveSessionSkuInfos(sessionData);
    }
  }

  // 返回当前时间可以参与的秒杀商品信息
  @Override
  public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
    // 1. 确定当前时间属于那个秒杀场次。
    long time = new Date().getTime();

    Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
    for (String key : keys) {
      String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
      String[] s = replace.split("_");
      long start = Long.parseLong(s[0]);
      long end = Long.parseLong(s[1]);
      if (time >= start && time <= end) {
        // 2. 获取这个秒杀场次需要的所有商品信息           // 说明这个商品当要使用的
        List<String> range = redisTemplate.opsForList().range(key, 0, -1);
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        List<String> list = hashOps.multiGet(range);
        if (list != null) {
          List<SecKillSkuRedisTo> collect = list.stream().map(item -> {
            SecKillSkuRedisTo redis = JSON.parseObject((String) item, SecKillSkuRedisTo.class);
//            redis.setRandomCode(null); // 当前秒杀开始了，就需要随机码
            return redis;
          }).collect(Collectors.toList());
          return collect;
        }
        break;
      }
    }
    return null;
  }

  @Override
  public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
    // 1. 找到所有需要参与秒杀的商品的key
    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
    Set<String> keys = hashOps.keys();
    if (keys != null && keys.size() > 0) {
      String regx = "\\d_" + skuId;
      for (String key : keys) {
        // 6_4
        if (Pattern.matches(regx, key)) {
          String json = hashOps.get(key);
          SecKillSkuRedisTo skuRedisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
          // 随机码
          long current = new Date().getTime();
          if (current >= skuRedisTo.getStartTime() && current <= skuRedisTo.getEntTime()) {
          } else {
            skuRedisTo.setRandomCode(null);
          }
          return skuRedisTo;
        }
      }
    }
    return null;
  }

  // TODO 上架秒杀商品的时候，每一个数据都有过期时间。
  // TODO 秒杀后续的流程，简化了收货地址等信息。
  @Override
  public String kill(String killId, String key, Integer num) {
    MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
    // 1. 获取当前秒杀商品的详细信息
    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

    String json = hashOps.get(killId);
    if (StringUtils.isEmpty(json)) {
      return null; // 没有这个秒杀商品
    }

    SecKillSkuRedisTo redis = JSON.parseObject(json, SecKillSkuRedisTo.class);
    // 校验合法性
    Long startTime = redis.getStartTime();
    Long entTime = redis.getEntTime();
    long time = new Date().getTime();
    long ttl = entTime - startTime;
    // 1. 校验时间的合法性
    if (time >= startTime && time <= entTime) {
      // 2. 校验随机码和商品id
      String randomCode = redis.getRandomCode();
      String skuId = redis.getPromotionSessionId() + "_" + redis.getSkuId();
      if (randomCode.equals(key) && killId.equals(skuId)) {
        // 3. 验证购物的数量是否合理
        if (num <= redis.getSeckillLimit()) {
          // 4. 验证这个人是否已经购买过。幂等性；如果只要秒杀成功，就去占位。  userId_SessionId_skuId
          String redisKey = respVo.getId() + "_" + skuId;
          // 自动过期
          Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);// setnx
          if (aBoolean) {
            // 占位成功声明从来没有买过
            RSemaphore semaphore = redissonClient.getSemaphore(SkU_STOCK_SEMAPHORE + randomCode);
            boolean b = semaphore.tryAcquire(num);// 尝试获取
            if (b) {
              // 快速下单。发送MQ消息
              System.out.println("发送MQ消息");
              String timeId = IdWorker.getTimeId();
              SeckillOrderTo orderTo = new SeckillOrderTo();
              orderTo.setOrderSn(timeId);
              orderTo.setMemberId(respVo.getId());
              orderTo.setNum(num);
              orderTo.setPromotionSessionId(redis.getPromotionSessionId());
              orderTo.setSkuId(redis.getSkuId());
              orderTo.setSeckillPrice(redis.getSeckillPrice());
              rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
              return timeId;
            }
          } else {
            // 说明已经买过了
            return null;
          }
        }
      }
    }

    return null;
  }

  private void savaSessionInfos(List<SeckillSessionsWithSkus> sessions) {
    if (sessions != null) {
      sessions.stream().forEach(session -> {
        Long startTime = session.getStartTime().getTime();
        Long endTime = session.getEndTime().getTime();
        String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
        // 缓存活动信息
        Boolean hasKey = redisTemplate.hasKey(key);
        if (!hasKey) {
          List<String> collect = session.getRelationSkus().stream()
              .map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString())
              .collect(Collectors.toList());
          if (collect != null && collect.size() > 0) {
            redisTemplate.opsForList().leftPushAll(key, collect);
          }
        }
      });
    }
  }

  private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
    sessions.stream().forEach(session -> {
      // 准备hash操作
      BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

      session.getRelationSkus().stream().forEach(seckillSkuVo -> {

        if (!operations.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString())) {
          // 缓存商品
          SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
          // 1. sku的基本数据
          R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
          if (skuInfo.getCode() == 0) {
            SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
            });

            redisTo.setSkuInfo(info);
          }

          // 2. sku的秒杀信息
          BeanUtils.copyProperties(seckillSkuVo, redisTo);

          // 3. 设置上当前商品的秒杀时间信息
          redisTo.setStartTime(session.getStartTime().getTime());
          redisTo.setEntTime(session.getEndTime().getTime());

          // 4. 随机码?      秒杀必须加上随机码
          String token = UUID.randomUUID().toString().replaceAll("-", "");
          redisTo.setRandomCode(token);

          String s = JSON.toJSONString(redisTo);
          operations.put(seckillSkuVo.getPromotionSessionId() + "_" + seckillSkuVo.getSkuId().toString(), s);

          // 如果当前这个场次的商品的库存信息已经上架就不需要上架了
          // 5. 使用库存作为分布式的信号量    限流
          RSemaphore semaphore = redissonClient.getSemaphore(SkU_STOCK_SEMAPHORE + token);
          // 商品可以秒杀的数量作为信号量
          semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
        }
      });
    });
  }
}
