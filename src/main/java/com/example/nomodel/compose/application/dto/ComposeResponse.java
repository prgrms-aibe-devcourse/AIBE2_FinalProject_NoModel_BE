package com.example.nomodel.compose.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComposeResponse(
        String status,                    // "success" | "error" | "processing"
        String message,                   // 상태 메시지
        String resultImageUrl,            // 합성된 이미지 URL (성공 시)
        Long resultFileId,               // 합성된 이미지 파일 ID (성공 시)
        String jobId,                    // 비동기 처리 시 작업 ID
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp,          // 응답 생성 시각
        
        ErrorDetail error                 // 에러 상세 정보 (실패 시)
) {
    // 성공 응답 생성
    public static ComposeResponse success(String resultImageUrl, Long resultFileId) {
        return new ComposeResponse(
            "success",
            "Image composition completed successfully",
            resultImageUrl,
            resultFileId,
            null,
            LocalDateTime.now(),
            null
        );
    }

    // 비동기 처리 응답 생성
    public static ComposeResponse processing(String jobId) {
        return new ComposeResponse(
            "processing",
            "Image composition is being processed",
            null,
            null,
            jobId,
            LocalDateTime.now(),
            null
        );
    }

    // 에러 응답 생성
    public static ComposeResponse error(String message, String errorCode, String errorDetail) {
        return new ComposeResponse(
            "error",
            message,
            null,
            null,
            null,
            LocalDateTime.now(),
            new ErrorDetail(errorCode, errorDetail)
        );
    }

    // 간단한 에러 응답 생성
    public static ComposeResponse error(String message) {
        return error(message, "COMPOSITION_ERROR", null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(
            String code,        // 에러 코드
            String detail       // 상세 에러 메시지
    ) {}
}