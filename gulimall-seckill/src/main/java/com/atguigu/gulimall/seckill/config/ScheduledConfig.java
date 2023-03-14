package com.atguigu.gulimall.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync  // 开启异步任务功能
@EnableScheduling  // 开启定时任务
public class ScheduledConfig {

}
