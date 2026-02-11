package com.mindlog.global.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var valueSerializer = RedisSerializationContext.SerializationPair.fromSerializer(
                new JdkSerializationRedisSerializer()
        );

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v2::")
                .serializeValuesWith(valueSerializer)
                .disableCachingNullValues();

        var cacheConfigs = Map.of(
                "emotionAnalysis",
                RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith("v2::")
                        .serializeValuesWith(valueSerializer)
                        .entryTtl(Duration.ofSeconds(90))
                        .disableCachingNullValues(),
                "monthlyDiaries",
                RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith("v2::")
                        .serializeValuesWith(valueSerializer)
                        .entryTtl(Duration.ofSeconds(60))
                        .disableCachingNullValues()
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
