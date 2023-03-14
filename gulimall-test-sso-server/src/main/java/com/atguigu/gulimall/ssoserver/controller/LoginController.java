package com.atguigu.gulimall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class LoginController {
  @Autowired
  private StringRedisTemplate redisTemplate;

  @ResponseBody
  @GetMapping("userInfo")
  public String userInfo(@RequestParam("token") String token) {
    return redisTemplate.opsForValue().get(token);
  }

  @GetMapping("/login.html")
  public String loginPage(@RequestParam("redirect_url") String url, Model model,
                          @CookieValue(value = "ss_token", required = false) String ss_token) {
    if (!StringUtils.isEmpty(ss_token)) {
      // 说明之前有人登录过，浏览器留下了痕迹
      return "redirect:" + url + "?token=" + ss_token;
    }
    model.addAttribute("url", url);
    return "login";
  }

  @PostMapping("/doLogin")
  public String doLogin (String username, String password,
                         String url, Model model,
                         HttpServletResponse response) {
    if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
      // 登录成功
      // 把登录成功的用户存起来
      String uuid = UUID.randomUUID().toString().replaceAll("-", "");
      redisTemplate.opsForValue().set(uuid, username);
      Cookie cookie = new Cookie("ss_token", uuid);
      response.addCookie(cookie);
      return "redirect:" + url + "?token=" + uuid;
    }
    model.addAttribute("url", url);
    // 登录失败。展示登录页
    return "login";
  }
}
