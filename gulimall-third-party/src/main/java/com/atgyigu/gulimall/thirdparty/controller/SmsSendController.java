package com.atgyigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.atgyigu.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sms")
public class SmsSendController {

  @Autowired
  private SmsComponent smsComponent;

  /**
   *  提供别的服务调用的           短信 验证码
   * @param phone  手机号
   * @param code   验证码
   * @return
   */
  @GetMapping("/sendcode")
  public R sendCode(@RequestParam("phone") String phone,
                    @RequestParam("code") String code) {
    try {
      smsComponent.sendSmsCode(phone, code);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return R.ok();
  }
}
