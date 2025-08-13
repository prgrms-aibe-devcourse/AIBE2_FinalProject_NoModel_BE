package com.example.nomodel._core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3.0 Configuration
 * 
 * API 문서화를 위한 설정 클래스
 * Spring Boot 3.x + springdoc-openapi-starter-webmvc-ui 사용
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * OpenAPI 3.0 설정
     * 
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                    createServer("http://localhost:" + serverPort + "/api", "Local Development Server"),
                    createServer("https://api.nomodel.com/api", "Production Server"),
                    createServer("https://dev-api.nomodel.com/api", "Development Server")
                ))
                .components(securityComponents())
                .addSecurityItem(securityRequirement());
    }

    /**
     * API 기본 정보 설정
     * 
     * @return API 정보 객체
     */
    private Info apiInfo() {
        return new Info()
                .title("NoModel API")
                .description("""
                    NoModel Spring Boot Application REST API Documentation
                    
                    ## 주요 기능
                    - Spring Boot 3.5.4 기반
                    - Spring Modulith 아키텍처
                    - JWT 인증/인가
                    - MySQL + Redis 연동
                    - Apache Kafka 이벤트 스트리밍
                    - Spring Batch 배치 처리
                    
                    ## 인증 방법
                    1. `/api/auth/login` 엔드포인트로 로그인
                    2. 응답으로 받은 JWT 토큰을 Authorization 헤더에 설정
                    3. `Bearer {token}` 형식으로 API 호출
                    
                    ## 환경 정보
                    - Profile: """ + activeProfile + """
                    - Port: """ + serverPort + """
                    """)
                .version("v1.0.0")
                .contact(apiContact())
                .license(apiLicense());
    }

    /**
     * API 연락처 정보
     * 
     * @return 연락처 정보 객체
     */
    private Contact apiContact() {
        return new Contact()
                .name("NoModel Development Team")
                .url("https://github.com/nomodel/nomodel");
    }

    /**
     * API 라이선스 정보
     * 
     * @return 라이선스 정보 객체
     */
    private License apiLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    /**
     * 서버 정보 생성
     * 
     * @param url 서버 URL
     * @param description 서버 설명
     * @return 서버 정보 객체
     */
    private Server createServer(String url, String description) {
        Server server = new Server();
        server.setUrl(url);
        server.setDescription(description);
        return server;
    }

    /**
     * 보안 컴포넌트 설정
     * JWT Bearer Token 인증 스키마 정의
     * 
     * @return 보안 컴포넌트 객체
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .name("Authorization")
                        .description("JWT Bearer Token Authentication")
                );
    }

    /**
     * 보안 요구사항 설정
     * 
     * @return 보안 요구사항 객체
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement()
                .addList("Bearer Authentication");
    }
}
