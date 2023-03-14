package com.atguigu.gulimall.member.service;

import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.atguigu.gulimall.member.vo.UserLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author happy
 * @email sunlightcs@gmail.com
 * @date 2022-12-15 14:40:41
 */
public interface MemberService extends IService<MemberEntity> {

  PageUtils queryPage(Map<String, Object> params);

  void regist(MemberRegistVo vo);

  void checkPhoneUnique(String phone) throws PhoneExistException;

  void checkUsernameUnique(String username) throws UsernameExistException;

  MemberEntity login(UserLoginVo vo);

  MemberEntity login(SocialUser socialUser) throws Exception;
}

