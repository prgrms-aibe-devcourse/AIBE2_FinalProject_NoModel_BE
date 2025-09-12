package com.example.nomodel.member.domain.service;

import com.example.nomodel.member.domain.model.LoginStatus;
import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginSecurityDomainService {

    private final LoginHistoryRepository loginHistoryRepository;
    
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int CHECK_MINUTES = 30;

    /**
     * IP 주소를 해시화
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
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash IP address", e);
            return "hash-error";
        }
    }

    /**
     * 특정 IP에서의 실패 횟수 확인 (brute force 공격 감지)
     */
    public boolean isIpBlocked(String ipAddress) {
        String hashedIp = hashIpAddress(ipAddress);
        LocalDateTime checkTime = LocalDateTime.now().minusMinutes(CHECK_MINUTES);
        
        long failedAttempts = loginHistoryRepository.countByHashedIpAndStatusAfter(
                hashedIp, LoginStatus.FAILURE, checkTime);
        
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            log.warn("Suspicious activity detected from IP (hashed): {}. Failed attempts: {}", 
                    hashedIp, failedAttempts);
            return true;
        }
        
        return false;
    }

    /**
     * 특정 회원의 비정상적인 로그인 패턴 감지
     */
    public boolean detectAnomalousLogin(Long memberId, String currentIpHash) {
        // 최근 성공한 로그인 내역 조회
        var recentLogins = loginHistoryRepository.findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(
                memberId, LocalDateTime.now().minusDays(7));
        
        if (recentLogins.isEmpty()) {
            return false;
        }
        
        // 새로운 IP에서의 로그인 감지
        boolean isNewLocation = recentLogins.stream()
                .filter(h -> h.getLoginStatus() == LoginStatus.SUCCESS)
                .noneMatch(h -> currentIpHash.equals(h.getHashedIp()));
        
        if (isNewLocation) {
            log.info("New location detected for member: {}", memberId);
            // 여기서 추가 검증이나 알림을 보낼 수 있음
        }
        
        return false; // 현재는 차단하지 않고 로깅만
    }
}