package com.example.nomodel.member.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.member.domain.model.LoginHistory;
import com.example.nomodel.member.domain.model.LoginStatus;
import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginSecurityDomainService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final StringRedisTemplate redisTemplate;
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int CHECK_MINUTES = 30;
    private static final String REDIS_KEY_PREFIX = "login_failures:";

    /**
     * IP 주소를 해시화
     * SHA-256은 JDK 표준 알고리즘이므로 예외 발생 불가
     */
    public String hashIpAddress(String ipAddress) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.SECURITY_ALGORITHM_NOT_AVAILABLE);
        }
    }

    /**
     * Redis 기반 IP 차단 여부 확인 (고성능)
     * @param ipAddress 확인할 IP 주소
     * @return 차단 여부
     */
    public boolean isIpBlocked(String ipAddress) {
        String hashedIp = hashIpAddress(ipAddress);
        String failureKey = REDIS_KEY_PREFIX + hashedIp;
        String failureCountStr = redisTemplate.opsForValue().get(failureKey);
        
        if (failureCountStr == null) {
            return false;
        }
        
        int failureCount = Integer.parseInt(failureCountStr);
        return failureCount >= MAX_FAILED_ATTEMPTS;
    }
    
    /**
     * IP 차단 확인 후 예외 발생
     * @param ipAddress 확인할 IP 주소
     * @throws ApplicationException IP가 차단된 경우
     */
    public void validateIpNotBlocked(String ipAddress) {
        if (isIpBlocked(ipAddress)) {
            String hashedIp = hashIpAddress(ipAddress);
            log.warn("Blocked IP attempted access: {}", hashedIp);
            throw new ApplicationException(ErrorCode.IP_BLOCKED);
        }
    }

    /**
     * 특정 회원의 비정상적인 로그인 패턴 감지 및 로깅
     * @param memberId 회원 ID
     * @param currentIpHash 현재 로그인 IP 해시
     */
    public void detectAnomalousLogin(Long memberId, String currentIpHash) {
        // 최근 7일간 성공한 로그인 내역 조회
        List<LoginHistory> recentSuccessLogins = loginHistoryRepository.findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(
                memberId, LocalDateTime.now().minusDays(7))
                .stream()
                .filter(h -> h.getLoginStatus() == LoginStatus.SUCCESS)
                .toList();
        
        // 첫 로그인이거나 최근 로그인이 없는 경우
        if (recentSuccessLogins.isEmpty()) {
            log.info("First login detected for member: {}", memberId);
            return;
        }
        
        // 새로운 IP 에서의 로그인 감지
        boolean isNewLocation = recentSuccessLogins.stream()
                .noneMatch(h -> currentIpHash.equals(h.getHashedIp()));
        
        if (isNewLocation) {
            log.info("New location login detected - Member: {}, IP hash: {}", memberId, currentIpHash);
            // TODO: 추가 보안 검증이나 사용자 알림 기능 구현 가능
        }
    }

    // ============= Redis 기반 IP 차단 관리 =============

    /**
     * 로그인 실패 시 Redis에 실패 카운트 증가
     * @param ipAddress 실패한 IP 주소
     */
    public void recordLoginFailure(String ipAddress) {
        String hashedIp = hashIpAddress(ipAddress);
        String failureKey = REDIS_KEY_PREFIX + hashedIp;
        
        // 실패 횟수 증가
        Long failureCountObj = redisTemplate.opsForValue().increment(failureKey);
        long failureCount = failureCountObj != null ? failureCountObj : 1L;
        
        // 첫 실패 시 TTL 설정
        if (failureCount == 1) {
            redisTemplate.expire(failureKey, Duration.ofMinutes(CHECK_MINUTES));
        }
        
        // 최대 실패 횟수 도달 시 로그
        if (failureCount >= MAX_FAILED_ATTEMPTS) {
            log.warn("IP blocked due to {} failed attempts: {}", failureCount, hashedIp);
        }
    }

    /**
     * 로그인 성공 시 Redis 에서 실패 기록 삭제
     * @param ipAddress 성공한 IP 주소
     */
    public void clearLoginFailures(String ipAddress) {
        String hashedIp = hashIpAddress(ipAddress);
        String failureKey = REDIS_KEY_PREFIX + hashedIp;
        redisTemplate.delete(failureKey);
    }
}