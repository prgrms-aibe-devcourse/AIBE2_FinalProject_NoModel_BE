package com.example.nomodel._core.config;

import com.example.nomodel._core.security.jwt.JWTTokenFilter;
import com.example.nomodel._core.security.jwt.JWTTokenProvider;
import com.example.nomodel._core.security.oauth2.CustomOAuth2UserService;
import com.example.nomodel._core.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.auth.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    private static final String[] WHITE_LIST = {
            "/",
            "/error",
            "/auth/**",
            "/qr/**",
            "/face/**",
            "/admin/kakao/token/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/health/**",
            "/actuator/**",
            "/h2-console/**",
            "/favicon.ico",
            "/test/**",          // 테스트 API 허용
            "/models/sync/**",   // 동기화 API 허용 (개발/테스트용)
            "/models/search/**", // 모델 검색 API 허용 (공개)
            "/oauth2/**",
            "/login/**",
            "/compose/**",
            "/api/compose/**",
            "/api/api/compose/**"
    };

    private static final String[] ADMIN_LIST = {
            "/admin/**"    // context-path 제거 (실제: /api/admin/**)
    };

    private static final String[] BAN_LIST = {
            "/vendor/**"
    };

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

                        // 잡 조회는 로컬에서 누구나 조회 가능
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/generate/jobs/**", "/generate/jobs/**").permitAll()

                        // Swagger 허용 (context-path 유무 모두 커버)
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
                                "/api/swagger-ui/**", "/api/v3/api-docs/**"
                        ).permitAll()

                        // 기존 화이트리스트 유지
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 관리자 권한 필요
                        .requestMatchers(ADMIN_LIST).hasRole("ADMIN")
                        // 금지된 경로
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
                // JWT 필터 활성화
                .addFilterBefore(new JWTTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        if (oauth2Enabled) {
            httpSecurity.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                    .successHandler(oAuth2SuccessHandler)
            );
        }

        return httpSecurity.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173", "https://aibe-2-final-project-no-model-fe.vercel.app/"));
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
