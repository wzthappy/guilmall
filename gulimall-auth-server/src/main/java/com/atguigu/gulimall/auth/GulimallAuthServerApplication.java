package com.atguigu.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 核心原理:
 *  1) @EnableRedisHttpSession 中导入了 RedisHttpSessionConfiguration
 *        1. 给容器中添加了一个组件
 *            RedisOperationsSessionRepository: redis操作session。session增删改查封装类
 *        2. SessionRepositoryFilter => Filter: session存储过滤器; 每个请求过来都必须经过filter
 *        	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                      request.setAttribute(SESSION_REPOSITORY_ATTR, this.sessionRepository);

                      SessionRepositoryRequestWrapper wrappedRequest = new SessionRepositoryRequestWrapper(request, response, this.servletContext);

                      SessionRepositoryResponseWrapper wrappedResponse = new SessionRepositoryResponseWrapper(wrappedRequest, response);

                      try {
                      filterChain.doFilter(wrappedRequest, wrappedResponse);
                      }
                      finally {
                      wrappedRequest.commitSession();
            }
                    包装了 (装饰者模式)
 *       3. 自动续期: redis中的数据也是有过期时间的
 */
@EnableFeignClients   // 开启feign远程调用功能
@SpringBootApplication
@EnableDiscoveryClient  // 开启nacos服务注册发现功能
@EnableRedisHttpSession // 整合redis作为session存储
public class GulimallAuthServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(GulimallAuthServerApplication.class, args);
  }

}
