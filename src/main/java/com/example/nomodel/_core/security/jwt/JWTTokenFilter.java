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

        // 토큰이 존재하고, refresh/logout 경로가 아닌 경우에만 인증 처리
        if (token != null && !isAuthExcludedPath(requestURI)) {
            // 토큰 유효성 검증
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context에 인증 정보 저장 완료: {}", authentication.getName());
                } catch (Exception e) {
                    log.error("인증 정보 설정 실패", e);
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * JWT 필터에서 제외할 경로 확인
     * refresh와 logout은 서비스 레이어에서 직접 토큰 검증
     */
    private boolean isAuthExcludedPath(String requestURI) {
        return requestURI.equals("/api/auth/refresh") || 
               requestURI.equals("/api/auth/logout");
    }
}
