package com.atguigu.gulimall.cart.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient("gulimall-product")
public interface ProductFeignService {

  @RequestMapping("/product/skuinfo/info/{skuId}")
  R getSkuInfo(@PathVariable("skuId") Long skuId);

  @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
  List<String> getSkuSaleAttrValues (@PathVariable("skuId") Long skuId);

  @GetMapping("/product/skuinfo/{skuId}/price")
  BigDecimal getPrice(@PathVariable("skuId") Long skuId);

  @PostMapping("/product/skuinfo/getSkuIdAndPrice")
  Map<Long, BigDecimal> getSkuIdAndPrice(@RequestBody List<Long> listIds);
}
