package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

/**
 * 定时任务:
 *    1. @EnableScheduling 开启定时任务
 *    2. @Scheduled(cron = "")  开启一个定时任务
 *
 * 异步任务:
 *    1. @EnableAsync 开启异步任务功能
 *    2. @Async 让这个方法异步执行
 *
 *    解决: 使用异步 + 定时任务来完成定时任务不阻塞的功能
 */
@Slf4j
@Component
//@EnableAsync  // 开启异步任务功能
//@EnableScheduling  // 开启定时任务
public class HelloSchedule {

  /**
   * 1. Spring中6位组成，不允许第7位的年
   * 2. 在周几的位置，1-7代表周一到周日；MON-SUN
   * 3. 定时任务不应该阻塞。默认是阻塞的
   *      1) 可以让运行以异步的方式，自己提交到线程池
   *      2) 支持定时任务线程池；设置 TaskScgedulingProperties
   *      3) 让定时任务异步执行
   */
//  @Async
//  @Scheduled(cron = "* * * ? * 4")
//  public void  hello () throws InterruptedException {
//    log.info("hello...");
//    Thread.sleep(3000);
//  }
}
