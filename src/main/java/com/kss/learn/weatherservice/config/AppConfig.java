package com.kss.learn.weatherservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Central cache configuration.
 *
 * Why a custom RedisCacheManager instead of relying purely on
 * application.properties defaults:
 *   1. We want JSON serialization (GenericJackson2JsonRedisSerializer) instead
 *      of the default JdkSerializationRedisSerializer, so cached values are
 *      human-readable in Redis and don't require entities to implement
 *      Serializable.
 *   2. We want an explicit, sane default TTL so entries don't live forever.
 *   3. We want the ability to give individual caches (e.g. "weather", "weatherList")
 *      different TTLs later without touching this class again.
 */
@Configuration
@EnableCaching
public class AppConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                );

        // Per-cache overrides go here.
        // "weather" (per-city forecast Strings) gets a 10-min TTL.
        // "weatherList" (the full getAllWeather() result) gets a shorter
        // TTL — it's a single cached entry covering the whole table, so a
        // shorter expiry limits how long it can drift from the DB in case
        // a write path's eviction is ever missed or a manual DB change is
        // made outside the app.
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("weather", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("weatherList", defaultConfig.entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * Explicit RedisTemplate<String, Object> bean.
     *
     * Spring Boot DOES auto-configure a RedisTemplate bean out of the box,
     * but it's typed RedisTemplate<Object, Object> (bean name "redisTemplate").
     * Because Spring's autowiring is generics-aware, that auto-configured
     * bean does NOT satisfy an injection point asking for
     * RedisTemplate<String, Object> — that mismatch is what originally caused:
     *
     *   "Parameter 0 of constructor in CacheInspectionService required a
     *    bean of type 'RedisTemplate' that could not be found"
     *
     * Defining this bean ourselves, with the exact generic type injected in
     * CacheInspectionService, fixes that. We also explicitly set
     * StringRedisSerializer for keys — matching exactly what RedisCacheManager
     * uses above — otherwise keys() pattern matching silently returns nothing
     * (the auto-configured template defaults to JdkSerializationRedisSerializer
     * for keys, which can't match the plain-text keys RedisCacheManager writes).
     * Values use the same JSON serializer as RedisCacheManager so reads here
     * deserialize consistently with what caching actually wrote.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

}