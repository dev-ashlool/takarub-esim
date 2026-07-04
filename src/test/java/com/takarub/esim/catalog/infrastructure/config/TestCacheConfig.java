package com.takarub.esim.catalog.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestCacheConfig {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                CacheConfig.CATALOG_COUNTRIES,
                CacheConfig.CATALOG_PACKAGES,
                CacheConfig.CATALOG_PACKAGE_DETAILS,
                CacheConfig.CATALOG_SEARCH);
    }
}
