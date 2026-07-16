package com.takarub.esim.catalog.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.infrastructure.config.CacheConfig;

/**
 * Evicts all catalog-related cache entries. Called after a successful catalog sync
 * so subsequent API requests fetch fresh data from the database.
 */
@Component
public class CatalogCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(CatalogCacheInvalidator.class);

    private static final String[] CATALOG_CACHE_NAMES = {
            CacheConfig.CATALOG_COUNTRIES,
            CacheConfig.CATALOG_PACKAGES,
            CacheConfig.CATALOG_PACKAGE_DETAILS,
            CacheConfig.CATALOG_SEARCH
    };

    private final CacheManager cacheManager;

    public CatalogCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void invalidateAll() {
        for (String cacheName : CATALOG_CACHE_NAMES) {
            try {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to clear catalog cache '{}': {}", cacheName, ex.getMessage());
            }
        }
        log.info("All catalog caches invalidated (catalog:*)");
    }
}
