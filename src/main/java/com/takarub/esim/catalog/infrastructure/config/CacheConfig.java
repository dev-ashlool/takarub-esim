package com.takarub.esim.catalog.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATALOG_COUNTRIES = "catalog:countries";
    public static final String CATALOG_PACKAGES = "catalog:packages";
    public static final String CATALOG_PACKAGE_DETAILS = "catalog:package-details";
    public static final String CATALOG_SEARCH = "catalog:search";

    /**
     * Redis-backed catalog cache. Enable only when Redis is available:
     * {@code takarub.cache.redis-enabled=true}.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "takarub.cache.redis-enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }

    /**
     * In-memory catalog cache for local/dev when Redis is not enabled (default).
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "takarub.cache.redis-enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
                CATALOG_COUNTRIES, CATALOG_PACKAGES, CATALOG_PACKAGE_DETAILS, CATALOG_SEARCH);
    }
}
