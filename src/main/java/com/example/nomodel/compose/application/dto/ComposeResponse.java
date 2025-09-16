package com.example.nomodel.compose.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComposeResponse(
        String jobId,                     // 작업 ID
        String status,                    // "SUCCEEDED" | "FAILED" | "PROCESSING"
        Integer inputFileId,              // 입력 파일 ID (null)
        Integer resultFileId,             // 결과 파일 ID
        String resultFileUrl,             // 결과 파일 URL
        String inputFileUrl,              // 입력 파일 URL (null)
        String errorMessage,              // 에러 메시지
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
        LocalDateTime createdAt,          // 생성 시각
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
        LocalDateTime updatedAt           // 업데이트 시각
) {
    // 성공 응답 생성
    public static ComposeResponse success(String jobId, Integer resultFileId, String resultFileUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "SUCCEEDED",
            null,          // inputFileId
            resultFileId,
            resultFileUrl,
            null,          // inputFileUrl
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }

    // 실패 응답 생성
    public static ComposeResponse failure(String jobId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "FAILED",
            null,          // inputFileId
            null,          // resultFileId
            null,          // resultFileUrl
            null,          // inputFileUrl
            errorMessage,
            now,           // createdAt
            now            // updatedAt
        );
    }

    // 처리 중 응답 생성
    public static ComposeResponse processing(String jobId) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "PROCESSING",
            null,          // inputFileId
            null,          // resultFileId
            null,          // resultFileUrl
            null,          // inputFileUrl
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }
}
