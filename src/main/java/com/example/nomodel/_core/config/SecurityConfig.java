package com.example.nomodel._core.config;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.jwt.JWTTokenFilter;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTTokenProvider jwtTokenProvider;

    private static final String[] WHITE_LIST = {
            "/",
            "/error",
            "/auth/**",          // context-path 제거 (실제: /api/auth/**)
            "/qr/**",            // context-path 제거 (실제: /api/qr/**)
            "/face/**",          // context-path 제거 (실제: /api/face/**)
            "/admin/kakao/token/**", // context-path 제거 (실제: /api/admin/kakao/token/**)
            "/swagger-ui/**",    // context-path 제거 (실제: /api/swagger-ui/**)
            "/v3/api-docs/**",   // context-path 제거 (실제: /api/v3/api-docs/**)
            "/swagger-ui.html",  // context-path 제거
            "/health/**",        // context-path 제거 (실제: /api/health/**)
            "/actuator/**",      // 이미 올바름
            "/h2-console/**",
            "/favicon.ico",
    };

    private static final String[] ADMIN_LIST = {
            "/admin/**"    // context-path 제거 (실제: /api/admin/**)
    };

    private static final String[] BAN_LIST = {
            "/vendor/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 프리플라이트 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // Actuator 허용(이미 있음)
                        .requestMatchers("/api/actuator/**", "/actuator/**").permitAll()

                        // 파일/생성 파이프라인 허용 (프론트 연동용)
                        .requestMatchers("/api/generate/**", "/api/files/**", "/generate/**", "/files/**").permitAll()

                        // 보안 화이트리스트
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()


                        // Swagger 허용 (context-path 유무 모두 커버)
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
                                "/api/swagger-ui/**", "/api/v3/api-docs/**"
                        ).permitAll()

                        // 기존 화이트리스트 유지
                        .requestMatchers(WHITE_LIST).permitAll()

                        // 관리자/차단 경로 규칙 유지
                        .requestMatchers(ADMIN_LIST).hasAuthority("ADMIN")
                        .requestMatchers(BAN_LIST).denyAll()

                        // 그 외 인증 필요
                        .anyRequest().authenticated()
                )
                .anonymous(anonymous -> anonymous.authorities("ROLE_ANONYMOUS"))

                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                )

                .exceptionHandling(ex -> {
                    ex.authenticationEntryPoint(authenticationEntryPoint());
                    ex.accessDeniedHandler(accessDeniedHandler());
                })

                // JWT 필터
                .addFilterBefore(new JWTTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // React 개발 서버 주소
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Authentication failed\",\"message\":\"" + authException.getMessage() + "\"}");
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
        };
    }
}