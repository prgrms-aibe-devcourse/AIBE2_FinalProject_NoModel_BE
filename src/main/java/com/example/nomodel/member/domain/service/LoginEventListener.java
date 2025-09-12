package com.example.nomodel.member.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.LoginHistory;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventListener {

    private final LoginHistoryRepository loginHistoryRepository;
    private final MemberJpaRepository memberRepository;
    private final LoginSecurityDomainService loginSecurityDomainService;

    @Async
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        CustomUserDetails userDetails = (CustomUserDetails) event.getAuthentication().getPrincipal();
        Member member = memberRepository.findById(userDetails.getMemberId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));
        
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = extractIpAddress(request);
        String hashedIp = loginSecurityDomainService.hashIpAddress(ipAddress);
        
        // 부가 기능들 (Redis 실패 기록 삭제, 이상 패턴 감지)
        loginSecurityDomainService.clearLoginFailures(ipAddress);
        loginSecurityDomainService.detectAnomalousLogin(member.getId(), hashedIp);
        
        // 핵심 기능 (실패 시 예외 전파)
        LoginHistory loginHistory = LoginHistory.createSuccessHistory(member, hashedIp);
        loginHistoryRepository.save(loginHistory);
        
        log.info("Login success recorded for member: {}", member.getId());
    }

    @Async
    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = extractIpAddress(request);
        String hashedIp = loginSecurityDomainService.hashIpAddress(ipAddress);
        
        // Redis 기반 실패 카운트 증가
        loginSecurityDomainService.recordLoginFailure(ipAddress);
        
        // 로그인 실패 이력 저장 (핵심 기능)
        String username = event.getAuthentication().getName(); // 사용자가 입력한 이메일
        String failureReason = "Invalid credentials for: " + username;
        
        LoginHistory loginHistory = LoginHistory.createFailureHistory(hashedIp, failureReason);
        
        // 입력된 이메일이 실제 회원과 연결 시도 (선택적)
        try {
            Email email = new Email(username);
            memberRepository.findByEmail(email).ifPresent(loginHistory::setMember);
        } catch (Exception e) {
            log.debug("Could not associate login failure with member: {}", username);
        }
        
        loginHistoryRepository.save(loginHistory);
        
        log.info("Login failure recorded for username: {}", username);
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            log.warn("Could not get HTTP request from context");
            return null;
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}