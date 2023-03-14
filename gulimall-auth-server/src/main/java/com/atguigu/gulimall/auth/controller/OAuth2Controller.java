package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

  @Autowired
  private MemberFeignService memberFeignService;

  /**
   * 社交登录成功回调
   */
  @GetMapping("/oauth2.0/weibo/success")
  public String weibo(@RequestParam("code") String code, HttpSession session,
                      HttpServletResponse servletResponse) throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("client_id", "1318372186");
    map.put("client_secret", "e43492d3f57ec2adcf702c0ac1509bee");
    map.put("grant_type", "authorization_code");
    map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
    map.put("code", code);
    Map<String, String> headers = new HashMap<String, String>();
    Map<String, String> querys = new HashMap<String, String>();
    // 1. 根据code换取access_Token
    HttpResponse response = HttpUtils.doPost("https://api.weibo.com",
        "/oauth2/access_token", "post", headers, querys, map);

    // 2. 处理
    if (response.getStatusLine().getStatusCode() == 200) {
      // 获取到了access_Token
      String json = EntityUtils.toString(response.getEntity());
      SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

      // 知道当前是那个社交用户
      // 1) 当前用户如果是第一次进入网站，自动注册进来(为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定的会员)
      // 登录或者注册社交用户
      R oauthLogin = memberFeignService.oauthLogin(socialUser);
      if (oauthLogin.getCode() == 0) {
        MemberRespVo data = oauthLogin.getData("data", new TypeReference<MemberRespVo>() {
        });
        log.info("登录成功用户: 用户信息{}", data.toString());
        // 1. 第一次使用session: 命令浏览器会自动保存一个JSESSIONID(Cookie)，
        //      在调用同域名接口的时候，他会自动带上这个Cookie，用他来查找对应自己的session对象
        // 子域之间: gulimall.com  auth.gulimall.com   order.gulimall.com
        //   在创建session时候，即使子域创建的，也能让父域直接使用。
        // TODO: 1. 默认发的的令牌(JsessionId -> session)的作用域: 当前域。 (解决子域JsessionId共享问题)
        // TODO: 2. 使用JSON的序列化方式来序列化对象数据到redis中

        session.setAttribute("loginUser", data);
//        new Cookie("JSESSIONID", "dadaa").setDomain("");
//        servletResponse.addCookie();
        // 2. 登录成功跳回首页
        return "redirect:http://gulimall.com";
      } else {
        // 失败
        return "redirect:http://auth.gulimall.com/login.html";
      }
    } else {
      // 失败
      return "redirect:http://auth.gulimall.com/login.html";
    }
  }
}
