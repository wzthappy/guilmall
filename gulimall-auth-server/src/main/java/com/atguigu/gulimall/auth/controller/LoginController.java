package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.ThirdPartFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import com.mysql.cj.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {
  /**
   * 发送一个请求直接跳转一个页面
   * SpringMvc viewcontroller；将请求和页面映射过来
   */

  @Autowired
  private ThirdPartFeignService thirdPartFeignService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private MemberFeignService memberFeignService;

  /**
   * 发送验证码
   *
   * @param phone 手机号
   */
  @ResponseBody
  @GetMapping("/sms/sendcode")
  public R sendCode(@RequestParam("phone") String phone) {
    // TODO 1. 接口防刷

    String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
    if (!StringUtils.isEmpty(redisCode)) {
      long l = Long.parseLong(redisCode.split("_")[1]);
      if (System.currentTimeMillis() - l < 60000) {
        // 60秒内不能在发
        return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
      }
    }

    // 2. 验证码的再次校验。 验证码存到redis中     key: sms:code:phone, value: code
    int shu = new Random().nextInt(99999);
    while (shu < 1000) shu = new Random().nextInt(99999);
    String code = String.valueOf(shu) + "_" + System.currentTimeMillis();   //  生成 验证码

    // redis缓存验证码，防止同一个phone在60秒内再次发送验证码
    redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
    code = String.valueOf(shu);
    thirdPartFeignService.sendCode(phone, code); // 手机号， 验证码
    return R.ok();
  }

  /**
   * // TODO 重定向携带数据，他利用session原理。将数据放在session中。
   * 只要跳到下一个页面取出这个数据以后，session里面的数据就会删掉
   * <p>
   * // TODO 1、分布式下的session问题。
   * RedirectAttributes: 模拟重定向携带数据
   */
  @PostMapping("/regist")
  public String register(@Valid UserRegistVo vo, BindingResult result,
                         RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {  // BindingResult result中是校验的结果
      /*
      result.getFieldErrors().stream()
          .collect(Collectors.toMap(fieldError -> {
            return fieldError.getField();
          }, fieldError -> {
            return fieldError.getDefaultMessage();
          }));
       */
      Map<String, String> errors = new HashMap<>();
      for (FieldError fieldError : result.getFieldErrors()) {
        errors.put(fieldError.getField(), fieldError.getDefaultMessage());
      }
      redirectAttributes.addFlashAttribute("errors", errors);
      // 校验出错，转发到注册页
      return "redirect:http://auth.gulimall.com/reg.html";
    }

    // 1. 校验验证码
    String code = vo.getCode();
    String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
    if (!StringUtils.isEmpty(s) && code.equals(s.split("_")[0])) {
      // 删除验证码; 令牌机制
      redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

      // 验证码通过。 正真注册。调用远程服务进程注册
      R r = memberFeignService.regist(vo);

      if (r.getCode() == 0) {
        // 成功
        // 注册成功回到首页，回到登录页
        return "redirect:http://auth.gulimall.com/login.html";
      } else {
        Map<String, String> errors = new HashMap<>();
        errors.put("msg", r.getData("msg", new TypeReference<String>() {}));
        redirectAttributes.addFlashAttribute("errore", errors);
        return "redirect:http://auth.gulimall.com/reg.html";
      }

    } else {
      Map<String, String> errors = new HashMap<>();
      errors.put("code", "验证码错误");

      redirectAttributes.addFlashAttribute("errors", errors);
      // 校验出错，转发到注册页
      return "redirect:http://auth.gulimall.com/reg.html";
    }
  }

  @GetMapping("/login.html")
  public String loginPage(HttpSession session) {
    Object attribute = session.getAttribute(AuthServerConstant.LONGIN_USER);
    if (attribute == null) {
      // 没登录
      return "login";
    } else {
      // 登录了
      return "redirect:http://gulimall.com";
    }
  }

  @PostMapping("/login")
  public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session) {
    // 远程登录
    R login = memberFeignService.login(vo);

    if (login.getCode() == 0) {
      MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
      });
      // 登录成功放到sesion中
      session.setAttribute(AuthServerConstant.LONGIN_USER, data);
      return "redirect:http://gulimall.com";
    } else {
      Map<String, String> errors = new HashMap<>();
      errors.put("msg", login.getData("msg", new TypeReference<String>() {}));
      redirectAttributes.addFlashAttribute("errors", errors);
      return "redirect:http://auth.gulimall.com/login.html";
    }
  }
}
