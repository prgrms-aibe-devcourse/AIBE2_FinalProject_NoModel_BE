package com.example.nomodel.member.infrastructure.security;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.LoginHistory;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.service.LoginSecurityDomainService;
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
        try {
            CustomUserDetails userDetails = (CustomUserDetails) event.getAuthentication().getPrincipal();
            Member member = memberRepository.findById(userDetails.getMemberId())
                    .orElseThrow(() -> new IllegalStateException("Member not found"));
            
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = extractIpAddress(request);
            String hashedIp = loginSecurityDomainService.hashIpAddress(ipAddress);
            
            // 비정상적인 로그인 패턴 감지
            loginSecurityDomainService.detectAnomalousLogin(member.getId(), hashedIp);
            
            LoginHistory loginHistory = LoginHistory.createSuccessHistory(member, hashedIp);
            loginHistoryRepository.save(loginHistory);
            
            log.info("Login success recorded for member: {}", member.getId());
        } catch (Exception e) {
            log.error("Failed to record login success", e);
        }
    }

    @Async
    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        try {
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = extractIpAddress(request);
            String hashedIp = loginSecurityDomainService.hashIpAddress(ipAddress);
            
            // IP 차단 확인
            if (loginSecurityDomainService.isIpBlocked(ipAddress)) {
                log.warn("Blocked IP attempted login: {}", hashedIp);
            }
            
            String username = event.getAuthentication().getName();
            String failureReason = "Invalid credentials for: " + username;
            
            LoginHistory loginHistory = LoginHistory.createFailureHistory(hashedIp, failureReason);
            
            // Try to associate with member if exists
            try {
                Email email = new Email(username);
                memberRepository.findByEmail(email).ifPresent(loginHistory::setMember);
            } catch (Exception e) {
                log.debug("Could not associate login failure with member: {}", username);
            }
            
            loginHistoryRepository.save(loginHistory);
            
            log.info("Login failure recorded for username: {}", username);
        } catch (Exception e) {
            log.error("Failed to record login failure", e);
        }
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