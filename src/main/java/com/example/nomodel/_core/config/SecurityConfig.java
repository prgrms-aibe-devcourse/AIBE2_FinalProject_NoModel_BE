package com.example.nomodel._core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> {
            // 모든 환경에서 항상 허용
            authz.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                 .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
            
            // 개발 환경에서만 허용 (local, dev, test, monitoring 프로필)
            if (isDevelopmentProfile()) {
                authz.requestMatchers("/h2-console/**").permitAll()
                     .requestMatchers("/api/test/**").permitAll();
            }
            
            // 나머지 모든 요청은 인증 필요
            authz.anyRequest().authenticated();
        });

        // CSRF 설정
        http.csrf(csrf -> {
            csrf.ignoringRequestMatchers("/actuator/**")
                .ignoringRequestMatchers("/swagger-ui/**", "/v3/api-docs/**");
            
            // 개발 환경에서만 추가 CSRF 제외
            if (isDevelopmentProfile()) {
                csrf.ignoringRequestMatchers("/h2-console/**", "/api/test/**");
            }
        });

        // Headers 설정
        http.headers(headers -> {
            if (isDevelopmentProfile()) {
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin()); // H2 Console용
            } else {
                headers.frameOptions(frameOptions -> frameOptions.deny()); // Production에서는 더 엄격하게
            }
        });

        return http.build();
    }

    /**
     * 개발 환경인지 확인하는 메서드
     */
    private boolean isDevelopmentProfile() {
        return activeProfile.contains("local") || 
               activeProfile.contains("dev") || 
               activeProfile.contains("test") || 
               activeProfile.contains("monitoring");
    }
}