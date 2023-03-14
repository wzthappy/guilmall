package com.atguigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {
  /**
   *  视图映射        跳转页面          不需要携带数据的页面，可以通以下方式来简单解决
   */
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    /**
     *   @GetMapping("/login.html")
     *   public String loginPage() {
     *     return "login";
     *   }
     *   @GetMapping("/reg.html")
     *   public String regPage() {
     *     return "reg";
     *   }
     */
//    registry.addViewController("/login.html").setViewName("login");
    registry.addViewController("/reg.html").setViewName("reg");
  }
}
