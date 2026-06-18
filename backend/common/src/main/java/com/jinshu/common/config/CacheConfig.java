package com.jinshu.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置。
 *
 * 缓存命名规范：jinshu:{tenantId}:{cacheName}:{key}
 * 实际由调用方通过 CacheKeyBuilder 拼接 tenantId 前缀。
 *
 * 默认过期时间：10 分钟
 * 配置类缓存：5 分钟
 * 空值缓存：5 分钟（防穿透）
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "jinshu.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    public static final String CACHE_TENANT = "tenant";
    public static final String CACHE_REPORT_METADATA = "reportMetadata";
    public static final String CACHE_USER_PERMISSIONS = "userPermissions";
    public static final String CACHE_DATA_SOURCE = "dataSource";
    public static final String CACHE_DICT = "dict";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put(CACHE_TENANT, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configMap.put(CACHE_REPORT_METADATA, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configMap.put(CACHE_USER_PERMISSIONS, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configMap.put(CACHE_DATA_SOURCE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configMap.put(CACHE_DICT, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()
                .build();
    }
}
