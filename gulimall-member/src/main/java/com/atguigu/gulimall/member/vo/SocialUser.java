package com.atguigu.gulimall.member.vo;

import lombok.Data;

@Data
public class SocialUser {
  private String access_token;
  private String expires_in;
  private String remind_in;
  private String uid;
}