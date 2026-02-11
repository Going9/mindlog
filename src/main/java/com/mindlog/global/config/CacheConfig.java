package com.mindlog.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        var valueSerializer = RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)
        );

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(valueSerializer)
                .disableCachingNullValues();

        var cacheConfigs = Map.of(
                "emotionAnalysis",
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(valueSerializer)
                        .entryTtl(Duration.ofSeconds(90))
                        .disableCachingNullValues(),
                "monthlyDiaries",
                RedisCacheConfiguration.defaultCacheConfig()
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
