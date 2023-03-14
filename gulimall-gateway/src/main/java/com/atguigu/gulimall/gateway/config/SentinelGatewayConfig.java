package com.atguigu.gulimall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SentinelGatewayConfig {

  public SentinelGatewayConfig() {
    GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
      // 网关限流了请求，就会调用此回调
      @Override
      public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
        Map<String, Map> map = new HashMap<String, Map>();
        Map<String, Object> a = new HashMap<String, Object>();
        a.put("code", 10002);
        a.put("mge", "请求流量过大");
        map.put("error", a);
        String errJson = JSON.toJSONString(map);

        Mono<ServerResponse> body = ServerResponse.ok().body(Mono.just(errJson), String.class);
        return body;
      }
    });
  }
}
