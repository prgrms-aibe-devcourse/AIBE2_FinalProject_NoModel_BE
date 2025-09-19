package com.example.nomodel._core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 * 모델 검색 결과 캐싱을 위한 Redis Cache Manager 구성
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * 메인 캐시 매니저 - 다양한 TTL 설정을 가진 캐시들 관리
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // JSON 직렬화를 위한 ObjectMapper 구성
        ObjectMapper objectMapper = createCacheObjectMapper();

        // 기본 캐시 설정 (TTL: 10분)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 모델 검색 결과 캐시 (자주 조회되는 첫 몇 페이지)
        cacheConfigurations.put("modelSearch", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));  // 30분 캐싱

        // 인기 모델 캐시 (변경이 적음)
        cacheConfigurations.put("popularModels", defaultConfig
                .entryTtl(Duration.ofHours(1)));  // 1시간 캐싱

        // 최신 모델 캐시 (자주 변경됨)
        cacheConfigurations.put("recentModels", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));  // 5분 캐싱

        // 추천 모델 캐시
        cacheConfigurations.put("recommendedModels", defaultConfig
                .entryTtl(Duration.ofHours(2)));  // 2시간 캐싱

        // 관리자 모델 캐시
        cacheConfigurations.put("adminModels", defaultConfig
                .entryTtl(Duration.ofHours(6)));  // 6시간 캐싱

        // 무료 모델 캐시
        cacheConfigurations.put("freeModels", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));  // 30분 캐싱

        // 자동완성 제안 캐시
        cacheConfigurations.put("autoComplete", defaultConfig
                .entryTtl(Duration.ofHours(1)));  // 1시간 캐싱

        // 모델 상세 캐시 (개별 모델)
        cacheConfigurations.put("modelDetail", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));  // 15분 캐싱

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * 짧은 TTL을 가진 캐시 매니저 (실시간성이 중요한 데이터)
     */
    @Bean
    public CacheManager shortLivedCacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper objectMapper = createCacheObjectMapper();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))  // 1분 TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 사용자별 개인화된 검색 결과 (짧은 캐싱)
        cacheConfigurations.put("userSearchResults", config
                .entryTtl(Duration.ofMinutes(2)));

        // 실시간 통계 (매우 짧은 캐싱)
        cacheConfigurations.put("realtimeStats", config
                .entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * 캐시용 ObjectMapper 생성
     * 타입 정보를 포함하여 역직렬화 시 정확한 타입 복원
     */
    private ObjectMapper createCacheObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Java Time 모듈 등록
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 모든 필드를 직렬화
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 타입 정보 포함 (캐시에서 복원 시 정확한 타입으로)
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }
}