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
        
        String ipAddress = extractIpFromAuthentication(event.getAuthentication());
        String hashedIp = loginSecurityDomainService.hashIpAddress(ipAddress);
        
        // 보안 관련 처리 (실패 기록 삭제, 이상 패턴 감지)
        loginSecurityDomainService.clearLoginFailures(ipAddress);
        loginSecurityDomainService.detectAnomalousLogin(member.getId(), hashedIp);
        
        // 로그인 성공 이력 저장
        LoginHistory loginHistory = LoginHistory.createSuccessHistory(member, hashedIp);
        loginHistoryRepository.save(loginHistory);
        
        log.info("Login success recorded for member: {}", member.getId());
    }

    @Async
    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String ipAddress = extractIpFromAuthentication(event.getAuthentication());
        String hashedIp = loginSecurityDomainService.hashIpAddress(ipAddress);
        String username = event.getAuthentication().getName();
        
        // 보안 관련 처리 (실패 카운트 증가)
        loginSecurityDomainService.recordLoginFailure(ipAddress);
        
        // 로그인 실패 이력 저장
        LoginHistory loginHistory = LoginHistory.createFailureHistory(hashedIp, 
                "Invalid credentials for: " + username);
        
        // 이메일이 유효한 회원과 연결 시도
        associateFailureWithMember(loginHistory, username);
        
        loginHistoryRepository.save(loginHistory);
        log.info("Login failure recorded for username: {}", username);
    }
    
    /**
     * Authentication 객체에서 IP 주소 추출
     * @param authentication 인증 객체
     * @return IP 주소
     */
    private String extractIpFromAuthentication(org.springframework.security.core.Authentication authentication) {
        String ipAddress = (String) authentication.getDetails();
        if (ipAddress == null) {
            // fallback: HTTP 요청에서 직접 추출
            HttpServletRequest request = loginSecurityDomainService.getHttpServletRequest();
            ipAddress = loginSecurityDomainService.extractIpAddress(request);
        }
        return ipAddress;
    }
    
    /**
     * 로그인 실패 이력을 회원과 연결
     * @param loginHistory 로그인 이력
     * @param username 사용자명 (이메일)
     */
    private void associateFailureWithMember(LoginHistory loginHistory, String username) {
        try {
            Email email = new Email(username);
            memberRepository.findByEmail(email).ifPresent(loginHistory::setMember);
        } catch (Exception e) {
            log.debug("Could not associate login failure with member: {}", username);
        }
    }
}