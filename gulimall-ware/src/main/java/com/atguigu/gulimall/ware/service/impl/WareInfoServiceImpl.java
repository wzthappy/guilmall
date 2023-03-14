package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.feign.MemberFeignService;
import com.atguigu.gulimall.ware.vo.FareVo;
import com.atguigu.gulimall.ware.vo.MemberAddressVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareInfoDao;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;
import com.atguigu.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

  @Autowired
  private MemberFeignService memberFeignService;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    String key = (String) params.get("key");
    QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
    if (!StringUtils.isEmpty(key)) {
      wrapper.eq("id", key)
          .or().like("name", key)
          .or().like("address", key)
          .or().like("areacode", key);
    }
    IPage<WareInfoEntity> page = this.page(
        new Query<WareInfoEntity>().getPage(params),
        wrapper
    );
    return new PageUtils(page);
  }

  @Override
  public FareVo getFare(Long addrId) {
    FareVo fareVo = new FareVo();
    R r = memberFeignService.addrInfo(addrId);
    MemberAddressVo data = r.getData("memberReceiveAddress" ,new TypeReference<MemberAddressVo>() {
    });
    if (data != null) {
      // 把手机号后1位数当成运费
      String phone = data.getPhone();
      String substring = phone.substring(phone.length() - 1, phone.length());
      BigDecimal bigDecimal = new BigDecimal(substring);
      fareVo.setAddress(data);
      fareVo.setFare(bigDecimal);
      return fareVo;
    }
    return null;
  }

}