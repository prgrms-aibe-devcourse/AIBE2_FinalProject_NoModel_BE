package com.example.nomodel.member.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 정보 응답")
public record UserInfoResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "가입일", example = "2024-01-15T10:30:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime joinedAt,

        @Schema(description = "요금제", allowableValues = {"free", "pro", "enterprise"}, example = "free")
        String planType,

        @Schema(description = "현재 포인트", example = "1000")
        Integer points,

        @Schema(description = "사용자 권한", allowableValues = {"USER", "ADMIN"}, example = "USER")
        String role,

        @Schema(description = "제작한 모델 수", example = "5")
        Long modelCount,

        @Schema(description = "생성한 프로젝트 수", example = "12")
        Long projectCount,

        @Schema(description = "최초 로그인 여부", example = "false")
        Boolean isFirstLogin
) {
}