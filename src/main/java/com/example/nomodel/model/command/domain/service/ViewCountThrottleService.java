package com.example.nomodel.model.command.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 조회수 증가 중복 방지 서비스
 * Redis를 활용하여 동일한 클라이언트의 중복 조회수 증가를 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountThrottleService {

    private final RedisTemplate<String, String> redisTemplate;
    
    // 중복 방지 시간 (5분)
    private static final Duration THROTTLE_DURATION = Duration.ofMinutes(5);
    
    // Redis 키 접두사
    private static final String THROTTLE_KEY_PREFIX = "view_throttle:";
    
    /**
     * 조회수 증가 가능 여부 확인
     * 
     * @param modelId 모델 ID
     * @param memberId 회원 ID
     * @return 조회수 증가 가능 여부
     */
    public boolean canIncrementViewCount(Long modelId, Long memberId) {
        String throttleKey = generateThrottleKey(modelId, memberId);
        
        try {
            // 이미 존재하면 중복 요청
            if (redisTemplate.hasKey(throttleKey)) {
                log.debug("조회수 증가 중복 방지: modelId={}, memberId={}", modelId, memberId);
                return false;
            }
            
            // 키 생성 및 TTL 설정 (5분간 중복 방지)
            redisTemplate.opsForValue().set(throttleKey, "1", THROTTLE_DURATION);
            log.debug("조회수 증가 허용: modelId={}, memberId={}, TTL={}분", 
                     modelId, memberId, THROTTLE_DURATION.toMinutes());
            return true;
            
        } catch (Exception e) {
            // Redis 오류 시 조회수 증가 허용 (가용성 우선)
            log.warn("Redis 조회수 중복 방지 오류 (조회수 증가 허용): modelId={}, error={}", 
                    modelId, e.getMessage());
            return true;
        }
    }
    
    
    /**
     * 중복 방지용 Redis 키 생성
     */
    private String generateThrottleKey(Long modelId, Long memberId) {
        return THROTTLE_KEY_PREFIX + modelId + ":" + memberId;
    }
    
}