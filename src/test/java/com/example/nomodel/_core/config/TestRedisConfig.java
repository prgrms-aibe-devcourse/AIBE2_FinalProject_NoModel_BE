package com.example.nomodel._core.config;

import com.example.nomodel.member.domain.repository.FirstLoginRedisRepository;
import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 테스트용 Redis 설정
 * test 프로필에서만 활성화되며, Redis 관련 Bean들을 Mock으로 대체합니다.
 *
 * Mock되는 컴포넌트:
 * - RedisConnectionFactory
 * - RedisTemplate
 * - StringRedisTemplate
 * - RefreshTokenRedisRepository
 * - FirstLoginRedisRepository
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfig {

    @Bean @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean @Primary
    public RefreshTokenRedisRepository refreshTokenRedisRepository() {
        return Mockito.mock(RefreshTokenRedisRepository.class);
    }

    @Bean
    @Primary
    public FirstLoginRedisRepository firstLoginRedisRepository() {
        return Mockito.mock(FirstLoginRedisRepository.class);
    }
}