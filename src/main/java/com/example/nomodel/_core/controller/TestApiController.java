package com.example.nomodel._core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Test API Controller for Swagger Documentation Demo
 * Swagger/OpenAPI 문서화 데모를 위한 테스트 API 컨트롤러
 */
@Tag(name = "Test API", description = "테스트 및 데모용 API")
@RestController
@RequestMapping("/test")
public class TestApiController {

    /**
     * Health Check API
     * 시스템 상태를 확인하는 API
     */
    @Operation(
        summary = "Health Check",
        description = "시스템 상태를 확인합니다. 인증이 필요하지 않습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "정상 응답",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = "{\"status\": \"UP\", \"message\": \"System is running\", \"timestamp\": \"2024-01-01T12:00:00\"}"
                )
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "System is running",
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Echo API
     * 입력받은 메시지를 그대로 반환하는 API
     */
    @Operation(
        summary = "Echo Message",
        description = "입력받은 메시지를 그대로 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 응답"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(
        @Parameter(description = "반환할 메시지", example = "Hello World")
        @RequestParam String message
    ) {
        return ResponseEntity.ok(Map.of(
            "echo", message,
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Secured API (JWT Token Required)
     * JWT 토큰이 필요한 보안 API
     */
    @Operation(
        summary = "Secured Endpoint",
        description = "JWT 토큰 인증이 필요한 보안 엔드포인트입니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정상 응답 (인증됨)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/secured")
    public ResponseEntity<Map<String, Object>> secured() {
        return ResponseEntity.ok(Map.of(
            "message", "You are authenticated!",
            "timestamp", LocalDateTime.now(),
            "user", "authenticated-user"
        ));
    }

    /**
     * Create User API (POST Example)
     * 사용자 생성 API (POST 예시)
     */
    @Operation(
        summary = "Create User",
        description = "새로운 사용자를 생성합니다.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "사용자 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "생성할 사용자 정보",
            content = @Content(
                examples = @ExampleObject(
                    value = "{\"name\": \"홍길동\", \"email\": \"hong@example.com\", \"age\": 30}"
                )
            )
        )
        @RequestBody Map<String, Object> userRequest
    ) {
        return ResponseEntity.ok(Map.of(
            "id", 1001,
            "name", userRequest.get("name"),
            "email", userRequest.get("email"),
            "created", LocalDateTime.now(),
            "message", "User created successfully"
        ));
    }

    /**
     * System Info API
     * 시스템 정보를 반환하는 API
     */
    @Operation(
        summary = "System Information",
        description = "시스템 정보를 조회합니다."
    )
    @GetMapping("/system-info")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        return ResponseEntity.ok(Map.of(
            "application", "NoModel",
            "version", "v1.0.0",
            "springBoot", "3.5.4",
            "java", System.getProperty("java.version"),
            "profile", System.getProperty("spring.profiles.active", "default"),
            "timestamp", LocalDateTime.now()
        ));
    }
}