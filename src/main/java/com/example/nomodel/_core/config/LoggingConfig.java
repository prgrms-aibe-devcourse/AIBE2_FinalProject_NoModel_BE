package com.example.nomodel._core.config;

import com.example.nomodel._core.logging.LoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 로깅 관련 설정 클래스
 * ELK Stack 최적화를 위한 MDC 필터 등록
 */
@Configuration
public class LoggingConfig {

    /**
     * LoggingFilter를 가장 높은 우선순위로 등록
     * 모든 요청에 대해 MDC 컨텍스트를 설정
     */
    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration(LoggingFilter loggingFilter) {
        FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>();
        
        registration.setFilter(loggingFilter);
        registration.addUrlPatterns("/*");
        registration.setName("LoggingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // 가장 높은 우선순위
        
        return registration;
    }
}