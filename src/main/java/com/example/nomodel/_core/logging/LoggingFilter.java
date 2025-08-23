package com.example.nomodel._core.logging;

import com.example.nomodel._core.security.CustomUserDetails;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * ELK Stack 로깅을 위한 MDC(Mapped Diagnostic Context) 관리 필터
 * 모든 요청에 대해 추적 가능한 컨텍스트 정보를 설정
 */
@Slf4j
@Component
public class LoggingFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String USER_ID = "userId";
    private static final String USER_NAME = "userName";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_AGENT = "userAgent";
    private static final String SESSION_ID = "sessionId";
    private static final String REQUEST_URI = "requestUri";
    private static final String HTTP_METHOD = "httpMethod";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // 요청 추적을 위한 컨텍스트 설정
            setupLoggingContext(httpRequest);
            
            // 다음 필터로 진행
            chain.doFilter(request, response);
            
        } finally {
            // MDC 정리 (메모리 누수 방지)
            clearLoggingContext();
        }
    }

    /**
     * 로깅 컨텍스트 설정
     */
    private void setupLoggingContext(HttpServletRequest request) {
        // Trace ID 생성 또는 추출 (분산 추적용)
        String traceId = extractOrGenerateTraceId(request);
        MDC.put(TRACE_ID, traceId);
        
        // Span ID 생성 (현재 요청 식별용)
        String spanId = generateSpanId();
        MDC.put(SPAN_ID, spanId);
        
        // HTTP 요청 정보
        MDC.put(HTTP_METHOD, request.getMethod());
        MDC.put(REQUEST_URI, request.getRequestURI());
        
        // 클라이언트 정보
        MDC.put(CLIENT_IP, extractClientIp(request));
        MDC.put(USER_AGENT, extractUserAgent(request));
        
        // 세션 정보
        String sessionId = extractSessionId(request);
        if (sessionId != null) {
            MDC.put(SESSION_ID, sessionId);
        }
        
        // 사용자 정보 (인증된 경우)
        setupUserContext();
    }

    /**
     * 사용자 컨텍스트 설정
     */
    private void setupUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            
            if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                MDC.put(USER_ID, String.valueOf(userDetails.getMemberId()));
                MDC.put(USER_NAME, userDetails.getUsername());
            } else {
                MDC.put(USER_NAME, authentication.getName());
                MDC.put(USER_ID, "UNKNOWN");
            }
        } else {
            MDC.put(USER_ID, "ANONYMOUS");
            MDC.put(USER_NAME, "ANONYMOUS");
        }
    }

    /**
     * Trace ID 추출 또는 생성
     * X-Trace-Id 헤더가 있으면 사용, 없으면 새로 생성
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
        }
        return traceId;
    }

    /**
     * Trace ID 생성
     */
    private String generateTraceId() {
        return "trace-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Span ID 생성
     */
    private String generateSpanId() {
        return "span-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 클라이언트 IP 추출
     * 프록시를 통한 요청도 고려
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }
        
        String xOriginalForwardedFor = request.getHeader("X-Original-Forwarded-For");
        if (xOriginalForwardedFor != null && !xOriginalForwardedFor.isEmpty() && 
            !"unknown".equalsIgnoreCase(xOriginalForwardedFor)) {
            return xOriginalForwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }

    /**
     * User Agent 추출
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "UNKNOWN";
    }

    /**
     * 세션 ID 추출
     */
    private String extractSessionId(HttpServletRequest request) {
        try {
            return request.getSession(false) != null ? request.getSession().getId() : null;
        } catch (Exception e) {
            log.debug("Failed to get session ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 로깅 컨텍스트 정리
     */
    private void clearLoggingContext() {
        MDC.clear();
    }
}