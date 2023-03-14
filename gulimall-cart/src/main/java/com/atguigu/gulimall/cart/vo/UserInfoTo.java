package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserInfoTo {
  private Long userId; // 登录了，用户ID
  private String userKey; // 没登录的token  - user-key  \  临时用户不管登没登录、都会有一个临时用户

  private boolean tempUser = false;  //  userKey中是否有值
}
