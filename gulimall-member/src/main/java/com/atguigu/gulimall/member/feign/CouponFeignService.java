package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 这是一个声明式的远程调用
 */
@FeignClient("gulimall-coupon")  // 要调用的服务的名称
public interface CouponFeignService {
  @RequestMapping("/coupon/coupon/member/list")  // 调用的这个服务的地址
  R membercoupons ();

}
