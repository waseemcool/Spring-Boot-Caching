package com.kss.learn.weatherservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
public class CacheInspectionService {

//    @Autowired
//    private CacheManager cacheManager;
//
//    public void printCacheContents(String cacheName) {
//        Cache cache = cacheManager.getCache(cacheName);
//
//        if (cache != null) {
//            System.out.println("Cache Contents:");
//            System.out.println(Objects.requireNonNull(cache.getNativeCache()).toString());
//        }else {
//            System.out.println("No Such Cache: " + cacheName);
//        }
//    }

    /*
     * NOTE: With Redis as the cache provider, Cache.getNativeCache() on a
     * RedisCache returns the RedisCache wrapper itself (there is no in-JVM
     * native map to print) — its toString() does NOT show cache contents.
     * That approach only worked with the default ConcurrentMapCache, whose
     * native cache IS the backing ConcurrentHashMap.
     *
     * For Redis, we inspect entries directly through a RedisTemplate instead,
     * RedisTemplate<String, Object> bean explicitly defined in AppConfig.
     * (Spring Boot's auto-configured RedisTemplate is typed <Object, Object>
     * AND defaults to JdkSerializationRedisSerializer for keys, which won't
     * match the plain-text keys RedisCacheManager writes — hence the
     * dedicated, correctly-serialized bean in AppConfig.)
     */
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheInspectionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void printCacheContents(String cacheName) {
        // Spring Cache keys in Redis are stored as "<cacheName>::<key>"
        Set<String> keys = redisTemplate.keys(cacheName + "::*");

        if (keys.isEmpty()) {
            System.out.println("No entries found in cache: " + cacheName);
            return;
        }

        System.out.println("Cache Contents for '" + cacheName + "':");
        for (String key : keys) {
            Object value = redisTemplate.opsForValue().get(key);
            System.out.println("  " + key + " -> " + value);
        }
    }

}