package com.atguigu.gulimall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1. 整合MyBatis-Plus
 *    1)、导入依赖
 *       <dependency>omidou</groupId>
 *         <artifactId>mybatis-plus-boot-starter</artifactId>
 *         <version>3.2.0</version>
 *       </dependency>
 *    2)、配置
 *        1、配置数据源:
 *            1)、导入数据库的驱动。
 *            2)、在application.yml配置数据源相关信息
 *        2、配置MyBatis-Plus:
 *            1)、使用@MapperScan
 *            2)、告诉MyBatis-Plus、sql映射文件位置
 * 2、逻辑删除
 *    1)、配置全局的删除逻辑规则(省略)
 *    2)、配置逻辑删除的组件Bean(省略)
 *    3)、加上逻辑删除注解@TableLogic
 *
 * 3. JSR303
 *    1)、给Bean添加校验注解: javax.validation.constraints，并定义自己的message提示
 *    2)、开启校验功能@Valid
 *      效果: 校验错误以后会有默认的响应；
 *   3)、给校验的bean后紧跟一个BindingResult，就可以获取到校验的结果
 *   4)、分组校验(多场景的复杂校验)
 *      1. @NotBlank(message = "品牌名必须提交", groups = {UpdateGrop.class, AddGroup.class})
 *          给校验注解标注什么情况需要进行校验
 *      2. @Validated({AddGroup.class})
 *      3. 默认没有指定分组的校验注解，在分组校验情况下不生效，他只会在@Validared({接口类.class})下生效
 *   5)、自定义校验
 *        1)、编写一个自定义的校验注解
 *        2)、编写一个自定义的校验器  ConstraintValidator
 *        3)、关联自定义的校验器和自定义的校验注解
 *            @Documented
 *            @Constraint(validatedBy = { ListValueConstraintValidator.class 【可以指定多个不同的校验器，适配不同类型的校验器】})
 *            @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
 *            @Retention(RUNTIME)
 *            public @interface ListValue {
 * 4. 统一的异常处理
 *    @ControllerAdvice
 *   1)、编写异常处理类，使用@ControllerAdvice
 *   2)、使用@ExceptionHandler标注方法可以处理的异常
 *
 * 5. 模板引擎
 *   1)、thymeleaf-starter: 关闭缓存
 *   2)、静态资源放在static文件夹下就可以按照路径直接访问
 *   3)、页面放在temolates下，直接访问
 *      SpringBoot 访问项目的时候，默认会找index
 *   4)、页面修改不重启服务器实时更新
 *        4.1 引入dev-tools
 *        4.2 修改完页面 controller shift f9 重启自动编译下页面， 代码配置 推荐重启
 *
 * 6. 整合redis
 *   1)、引入data-redis-starter
 *   2)、简单配置redis的host等信息
 *   3)、使用SpringBoot自动配置好的StringRedisTemplate来操作redis
 *
 * 7. 整合redisson作为分布式锁等功能框架
 *    1、引入依赖
 *        <dependency>
 *           <groupId>org.redisson</groupId>
 *           <artifactId>redisson</artifactId>
 *           <version>3.13.4</version>
 *        </dependency>
 *    2、配置redisson
 * 8. 整合SpringCache简化缓存开发
 *    1、引入依赖
 *        spring-boot-starter-cache、spring-boot-starter-data-redis
 *    2、写配置
 *        1)、自动配置了那些
 *          CacheAutoConfiguration会导入 RedisCacheConfiguration
 *          自动配好了缓存管理器RedisCacheManager
 *        2)、配置使用redis作为缓存
 *    3、测试使用缓存
 *          1) @EnableCaching  // 开启cache缓存功能
 *          2) 只需使用注解就能完成缓存操作
 *    4. Spring-Cache的不足:   是有脏数据问题的
 *         1)、读模式:
 *              缓存穿透: 查询一个null数据。解决: 缓存空数据  ；  cache默认开启空数据模式
 *              缓存击穿: 大量并发进来同时查询一个正好过期的数据的数据。  解决: 加锁(双重判断),  默认是没有加锁的。
 *                          在对应@Cacheable注解中添加 sync = true 就可以了(他加的是synchronized锁，也有双重判断)
 *                          其他注解没有
 *              缓存雪崩: 大量的key同时过期。  解决: 随机过期时间。 其实不加随机也是可以的，因为请求也是随机。
 *
 *         2)、写模式:  (缓存与数据库一致)
 *               1. 读写加锁。
 *               2. 引入Canal, 感知到MySQL的更新 实时 更新缓存
 *               3. 读多写多，直接去数据库查询就行
 *
 *      总结:
 *         常规数据(读多写少，及时性，一致性要求不高的数据)；完全可以用Spring-Cache
 *
 *         如果一个数据 想放入缓存 又 想强一致性  就可以使用Canal
 */
//@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication  // 主启动类
@EnableDiscoveryClient  // 开启nacos
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign") // 开启feign  远程调用功能
@EnableRedisHttpSession // 整合redis作为session存储
public class GulimallProductApplication {
  public static void main(String[] args) {
    SpringApplication.run(GulimallProductApplication.class, args);
  }
}
