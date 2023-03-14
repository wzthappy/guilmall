package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-15 14:53:45
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

  void updateOrderStatus(@Param("outTradeNo") String outTradeNo, @Param("code") Integer code);
}
