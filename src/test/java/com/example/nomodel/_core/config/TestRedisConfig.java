package com.example.nomodel._core.config;

import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 테스트용 Redis 설정
 * Redis 관련 빈들을 Mock으로 처리하여 실제 Redis 연결 없이 테스트 가능
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfig {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RefreshTokenRedisRepository refreshTokenRedisRepository;
}