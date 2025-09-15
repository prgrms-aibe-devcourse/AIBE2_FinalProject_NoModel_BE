package com.example.nomodel.member.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirstLoginRedisRepository 단위 테스트")
class FirstLoginRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private FirstLoginRedisRepository firstLoginRedisRepository;

    private final Long testMemberId = 1L;
    private final String expectedKey = "firstLogin:" + testMemberId;
    private final Duration expectedTTL = Duration.ofDays(30);

    @BeforeEach
    void setUp() {
        firstLoginRedisRepository = new FirstLoginRedisRepository(redisTemplate);
    }

    @Test
    @DisplayName("최초 로그인 상태 저장 - true")
    void setFirstLoginStatus_WithTrue_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        boolean isFirstLogin = true;
        String expectedValue = "true";

        // when
        firstLoginRedisRepository.setFirstLoginStatus(testMemberId, isFirstLogin);

        // then
        verify(valueOperations).set(expectedKey, expectedValue, expectedTTL);
    }

    @Test
    @DisplayName("최초 로그인 상태 저장 - false")
    void setFirstLoginStatus_WithFalse_Success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        boolean isFirstLogin = false;
        String expectedValue = "false";

        // when
        firstLoginRedisRepository.setFirstLoginStatus(testMemberId, isFirstLogin);

        // then
        verify(valueOperations).set(expectedKey, expectedValue, expectedTTL);
    }

    @Test
    @DisplayName("최초 로그인 여부 확인 - true 반환")
    void isFirstLogin_ReturnsTrue_WhenCacheHit() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn("true");

        // when
        Boolean result = firstLoginRedisRepository.isFirstLogin(testMemberId);

        // then
        assertThat(result).isTrue();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("최초 로그인 여부 확인 - false 반환")
    void isFirstLogin_ReturnsFalse_WhenCacheHit() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn("false");

        // when
        Boolean result = firstLoginRedisRepository.isFirstLogin(testMemberId);

        // then
        assertThat(result).isFalse();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("최초 로그인 여부 확인 - 캐시 미스")
    void isFirstLogin_ReturnsNull_WhenCacheMiss() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);

        // when
        Boolean result = firstLoginRedisRepository.isFirstLogin(testMemberId);

        // then
        assertThat(result).isNull();
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("최초 로그인 상태 삭제 - 성공")
    void deleteFirstLoginStatus_Success() {
        // given
        given(redisTemplate.delete(expectedKey)).willReturn(true);

        // when
        firstLoginRedisRepository.deleteFirstLoginStatus(testMemberId);

        // then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("최초 로그인 상태 삭제 - 키가 존재하지 않음")
    void deleteFirstLoginStatus_KeyNotExists() {
        // given
        given(redisTemplate.delete(expectedKey)).willReturn(false);

        // when
        firstLoginRedisRepository.deleteFirstLoginStatus(testMemberId);

        // then
        verify(redisTemplate).delete(expectedKey);
    }
}