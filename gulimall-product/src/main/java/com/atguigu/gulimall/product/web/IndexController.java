package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import jodd.util.ThreadUtil;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

  @Autowired
  private CategoryService categoryService;

  @Autowired
  private RedissonClient redisson;

  @GetMapping({"/", "/index.html"})
  public String StringindexPage(Model model) {
    // TODO 1. 查出所有的一级分类
    List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();

    // 默认前缀: classpath:/templates/
    // 默认后缀: .html
    model.addAttribute("categorys", categoryEntities);
    return "index";
  }

  @ResponseBody
  @GetMapping("/index/catalog.json")
  public Map<String, List<Catelog2Vo>>  getCatalogJson() {
    Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
    return map;
  }

  @ResponseBody
  @GetMapping("/hello")
  public String hello() {
    // 1. 获取一把锁，只要锁的名字一样，就是同一把锁
    RLock lock = redisson.getLock("my-lock");
    lock.lock();  // 堵塞式等待
    try {
      System.out.println("加锁成功，执行业务...." + Thread.currentThread().getId());
      TimeUnit.SECONDS.sleep(30);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      // 3. 解锁
      System.out.println("释放锁" + Thread.currentThread().getId());
      lock.unlock();
    }
    return "hello";
  }

  @GetMapping("/park")
  @ResponseBody
  public String park () throws InterruptedException {
    RSemaphore park = redisson.getSemaphore("park");
    park.acquire(); // 获取一个信号量
    return "ok";
  }

  @GetMapping("/go")
  @ResponseBody
  public String go () {
    RSemaphore park = redisson.getSemaphore("park");
    park.release(); // 释放一个车位
    return "ok";
  }

  @GetMapping("/lockDoor")
  @ResponseBody
  public String lockDoor() throws InterruptedException {
    RCountDownLatch door = redisson.getCountDownLatch("door");
    door.trySetCount(5);
    door.await(); // 等待闭锁都完成
    return "放假了...";
  }

  @GetMapping("/gogogo/{id}")
  public String gogogo(@PathVariable("id") Long id) {
    RCountDownLatch door = redisson.getCountDownLatch("door");
    door.countDown(); // 计数减一
    return id + "班的人都走了...";
  }
}
