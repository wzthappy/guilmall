package com.atguigu.gulimall.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;

@Data
@Controller
@ConfigurationProperties(prefix = "gulimall.thread")
public class ThreadPoolConfigProperties {
  private Integer coreSize;
  private Integer maxSize;
  private Integer keepAliveTime;
}
