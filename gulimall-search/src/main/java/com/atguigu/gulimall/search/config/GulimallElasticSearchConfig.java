package com.atguigu.gulimall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1. 导入依赖
 * 2. 编写配置，给容器中注入一个RestHighLevelClient
 * 3. 参照API
 */
@Configuration
public class GulimallElasticSearchConfig {
  public static final RequestOptions COMMON_OPTIONS;
  static {
    RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//    builder.addHeader();
//    builder.setHttpAsyncResponseConsumerFactory(
//        new HttpAsyncResponseConsumerFactory
//            .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024)
//    );
    COMMON_OPTIONS = builder.build();
  }

  @Bean
  public RestHighLevelClient esRestClient() {
    return new RestHighLevelClient(RestClient.builder(
        HttpHost.create("http://192.168.200.130:9200")
    ));
  }
}
