package com.rentalcar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {


    private static final Duration CAR_SEARCH_TTL   = Duration.ofMinutes(10);
    private static final Duration CAR_DETAILS_TTL  = Duration.ofMinutes(30);
    private static final Duration CITIES_TTL       = Duration.ofHours(1);

    public static final String CACHE_CAR_SEARCH    = "car-search";
    public static final String CACHE_CAR_DETAILS   = "car-details";
    public static final String CACHE_CITIES        = "cities";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .activateDefaultTyping(
                new com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
            );

        var jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            CACHE_CAR_SEARCH,   defaultConfig.entryTtl(CAR_SEARCH_TTL),
            CACHE_CAR_DETAILS,  defaultConfig.entryTtl(CAR_DETAILS_TTL),
            CACHE_CITIES,       defaultConfig.entryTtl(CITIES_TTL)
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
