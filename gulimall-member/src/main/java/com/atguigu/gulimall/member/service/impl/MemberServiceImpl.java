package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import com.atguigu.gulimall.member.vo.UserLoginVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

  @Autowired
  private MemberLevelDao memberLevelDao;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;

  @Override
  public PageUtils queryPage(Map<String, Object> params) {
    IPage<MemberEntity> page = this.page(
        new Query<MemberEntity>().getPage(params),
        new QueryWrapper<MemberEntity>()
    );

    return new PageUtils(page);
  }

  @Override
  public void regist(MemberRegistVo vo) {
    MemberEntity entity = new MemberEntity();
    // 设置默认等级
    MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
    entity.setLevelId(levelEntity.getId());

    // 检查用户名和手机号是否唯一。为了让controller能感知异常，  《异常机制》
    checkPhoneUnique(vo.getPhone());
    checkUsernameUnique(vo.getUserName());

    entity.setMobile(vo.getPhone());
    entity.setUsername(vo.getUserName());

    entity.setNickname(vo.getUserName());
    // 密码要进行加密存储
    String encode = passwordEncoder.encode(vo.getPassword());
    entity.setPassword(encode);

    // 其他默认信息

    // 保存
    baseMapper.insert(entity);
  }

  @Override
  public void checkPhoneUnique(String phone) throws PhoneExistException {
    MemberDao memberDao = this.baseMapper;
    Integer mobile = memberDao
        .selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
    if (mobile > 0) {
      throw new PhoneExistException();
    }
  }

  @Override
  public void checkUsernameUnique(String username) throws UsernameExistException {
    MemberDao memberDao = this.baseMapper;
    Integer count = memberDao
        .selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
    if (count > 0) {
      throw new UsernameExistException();
    }
  }

  @Override
  public MemberEntity login(UserLoginVo vo) {
    String loginacct = vo.getLoginacct();
    String password = vo.getPassword();

    // 1. 去数据库查询
    MemberDao memberDao = this.baseMapper;
    //    SELECT * FROM `ums_member` where  username = ? or mobile = ?
    MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>()
        .eq("username", loginacct).or().eq("mobile", loginacct));
    if (entity == null) {
      // 登录失败
      return null;
    } else {
      // 1. 获取到数据库的password
      String passwordDb = entity.getPassword();
      // 2. 密码匹配
      boolean matches = passwordEncoder.matches(password, passwordDb);
      if (matches) {
        // 登录成功
        return entity;
      } else {
        return null;
      }
    }
  }

  @Override
  public MemberEntity login(SocialUser socialUser) throws Exception {
    // 登录和注册合并逻辑
    String uid = socialUser.getUid();
    // 1. 判断当前社交用户是否已经登录过系统
    MemberDao memberDao = this.baseMapper;
    MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));

    if (memberEntity != null) {
      // 这个用户已经注册
      MemberEntity update = new MemberEntity();
      update.setId(memberEntity.getId());
      update.setAccessToken(socialUser.getAccess_token());
      update.setExpiresIn(socialUser.getExpires_in());
      memberDao.updateById(update);

      memberEntity.setAccessToken(socialUser.getAccess_token());
      memberEntity.setExpiresIn(socialUser.getExpires_in());
      return memberEntity;
    } else {
      // 没有查到当前社交用户对应的记录我们就需要注册一个
      MemberEntity regist = new MemberEntity();

      try {
        // 3. 查询当前社交用户的社交账号信息 (昵称，性别等)
        Map<String, String> query = new HashMap<>();
        query.put("access_token", socialUser.getAccess_token());
        query.put("uid", socialUser.getUid());
        HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json",
            "get", new HashMap<String, String>(), query);

        if (response.getStatusLine().getStatusCode() == 200) {
          // 查询成功
          String json = EntityUtils.toString(response.getEntity());
          JSONObject jsonObject = JSON.parseObject(json);
          // 昵称
          String name = jsonObject.getString("name");
          // 性别 m -> 男
          String gender = jsonObject.getString("gender");
          // .... 等等
          regist.setNickname(name);
          regist.setGender("m".equals(gender) ? 1 : 0);
        }
      } catch (Exception e) {}
      regist.setSocialUid(socialUser.getUid());
      regist.setAccessToken(socialUser.getAccess_token());
      regist.setExpiresIn(socialUser.getExpires_in());
      memberDao.insert(regist);

      return regist;
    }
  }
}