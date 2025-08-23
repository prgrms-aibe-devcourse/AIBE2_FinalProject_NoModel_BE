package com.example.nomodel._core.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JWTTokenFilter extends GenericFilterBean {

    private final JWTTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String requestURI = httpRequest.getRequestURI();
        
        log.debug("JWT Filter - Request URI: {}, Token exists: {}", requestURI, token != null);

        // WHITE_LIST에 포함된 경로는 JWT 인증을 건너뛴다
        if (isAuthExcludedPath(requestURI)) {
            log.debug("JWT Filter - Excluding authentication for white-listed path: {}", requestURI);
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 토큰이 존재하는 경우에만 인증 처리
        if (token != null) {
            // 토큰 유효성 검증
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context에 인증 정보 저장 완료: {}", authentication.getName());
                } catch (Exception e) {
                    log.error("인증 정보 설정 실패", e);
                    // 토큰이 유효하지 않으면 SecurityContext를 비운다
                    SecurityContextHolder.clearContext();
                }
            } else {
                // 토큰이 유효하지 않으면 SecurityContext를 비운다
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * JWT 필터에서 제외할 경로 확인
     * WHITE_LIST에 포함된 경로들은 JWT 인증을 건너뛴다
     */
    private boolean isAuthExcludedPath(String requestURI) {
        // refresh와 logout은 서비스 레이어에서 직접 토큰 검증
        if (requestURI.equals("/api/auth/refresh") || requestURI.equals("/api/auth/logout")) {
            return true;
        }
        
        // context-path를 고려하여 실제 요청 경로 체크
        String[] whiteList = {
            "/",
            "/error",
            "/api/auth/**",
            "/api/qr/**",
            "/api/face/**",
            "/api/admin/kakao/token/**",
            "/api/swagger-ui/**",
            "/api/v3/api-docs/**",
            "/swagger-ui.html",
            "/api/health/**",
            "/api/actuator/**",
            "/actuator/**",
            "/h2-console/**",
            "/favicon.ico"
        };
        
        for (String pattern : whiteList) {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (requestURI.startsWith(prefix)) {
                    return true;
                }
            } else if (pattern.equals(requestURI)) {
                return true;
            }
        }
        
        return false;
    }
}
