package com.example.nomodel.member.domain.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirstLoginRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    
    // Redis key prefix
    private static final String KEY_PREFIX = "firstLogin:";
    
    // TTL: 30일 (모든 경우 동일한 캐시 유지 기간)
    private static final Duration TTL = Duration.ofDays(30);

    /**
     * 최초 로그인 상태 저장 (Boolean 값 기반)
     * @param memberId 회원 ID
     * @param isFirstLogin 최초 로그인 여부
     */
    public void setFirstLoginStatus(Long memberId, boolean isFirstLogin) {
        String key = KEY_PREFIX + memberId;
        String value = isFirstLogin ? "true" : "false";
        
        redisTemplate.opsForValue().set(key, value, TTL);
        log.debug("최초 로그인 상태 저장: memberId={}, isFirstLogin={}, TTL={}일", memberId, isFirstLogin, TTL.toDays());
    }

    /**
     * 최초 로그인 여부 확인
     * @param memberId 회원 ID
     * @return 최초 로그인 여부 (null: 캐시 미스, true/false: 캐시된 값)
     */
    public Boolean isFirstLogin(Long memberId) {
        String key = KEY_PREFIX + memberId;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            log.debug("최초 로그인 캐시 미스: memberId={}", memberId);
            return null; // 캐시 미스 - DB 조회 필요
        }
        
        boolean isFirstLogin = "true".equals(value);
        log.debug("최초 로그인 캐시 히트: memberId={}, isFirstLogin={}", memberId, isFirstLogin);
        return isFirstLogin;
    }

    /**
     * 최초 로그인 상태 삭제 (명시적 삭제가 필요한 경우)
     * @param memberId 회원 ID
     */
    public void deleteFirstLoginStatus(Long memberId) {
        String key = KEY_PREFIX + memberId;
        Boolean deleted = redisTemplate.delete(key);
        if (deleted) {
            log.debug("최초 로그인 상태 삭제: memberId={}", memberId);
        }
    }
}