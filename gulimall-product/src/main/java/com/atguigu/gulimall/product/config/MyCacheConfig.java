package com.atguigu.gulimall.product.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * 配置文件中的东西没有用上
 *
 * 1. 和配置文件绑定的配置类不在容器中:
 * @ConfigurationProperties(prefix = "spring.cache")
 * public class CacheProperties {
 * 2. 要把他放入容器中
 */
@EnableCaching  // 开启cache缓存功能
@Configuration
@EnableConfigurationProperties(CacheProperties.class) // 1. 开启属性配置(这个类以开启)   2. 把这个类加入容器中
public class MyCacheConfig {
  @Bean
  public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
//    config = config.entryTtl();
    // 生成的缓存在redis中以json格式展示，默认是java序列化后的格式展示的
    config = config.serializeValuesWith(
        RedisSerializationContext.SerializationPair.
            fromSerializer(new GenericJackson2JsonRedisSerializer()));
    // 将配置文件中的所有配置都生效
    CacheProperties.Redis redisProperties = cacheProperties.getRedis();
    if (redisProperties.getTimeToLive() != null) {
      config = config.entryTtl(redisProperties.getTimeToLive());
    }
    if (redisProperties.getKeyPrefix() != null) {
      config = config.prefixKeysWith(redisProperties.getKeyPrefix());
    }
    if (!redisProperties.isCacheNullValues()) {
      config = config.disableCachingNullValues();
    }
    if (!redisProperties.isUseKeyPrefix()) {
      config = config.disableKeyPrefix();
    }
    return config;
  }
}
