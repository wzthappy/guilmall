package com.atguigu.gulimall.coupon.service.impl;

import com.atguigu.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.atguigu.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.coupon.dao.SeckillSessionDao;
import com.atguigu.gulimall.coupon.entity.SeckillSessionEntity;
import com.atguigu.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

  @Autowired
  private SeckillSkuRelationService seckillSkuRelationService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<SeckillSessionEntity> page = this.page(
        new Query<SeckillSessionEntity>().getPage(params),
        new QueryWrapper<SeckillSessionEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public List<SeckillSessionEntity> getLate3DaySession() {
    // 计算最近三天
    List<SeckillSessionEntity> list = this.list(
        new QueryWrapper<SeckillSessionEntity>()
            .between("start_time", startTime(), endTime())
    );

    if (list != null && list.size() > 0) {
      List<SeckillSessionEntity> collect = list.stream().map(session -> {
        Long id = session.getId();
        List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(
            new QueryWrapper<SeckillSkuRelationEntity>()
                .eq("promotion_session_id", id)
        );
        session.setRelationSkus(relationEntities);
        return session;
      }).collect(Collectors.toList());

      return collect;
    }
    return null;
  }

  // 起始时间  2023-03-09T00:00
  private String startTime() {
    LocalDate now = LocalDate.now();
    LocalTime min = LocalTime.MIN;
    LocalDateTime start = LocalDateTime.of(now, min);

    String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    return format;
  }

  // 结束时间  (最近3天时间)  2023-03-11T23:59:59.999999999
  private String endTime() {
    LocalDate now = LocalDate.now();
    LocalDate localDate = now.plusDays(2);
    LocalTime max = LocalTime.MAX;
    LocalDateTime end = LocalDateTime.of(localDate, max);

    String format = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    return format;
  }
}