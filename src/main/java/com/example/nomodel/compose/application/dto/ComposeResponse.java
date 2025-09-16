package com.example.nomodel.compose.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComposeResponse(
        String jobId,                     // 작업 ID
        String status,                    // "SUCCEEDED" | "FAILED" | "PROCESSING"
        Integer inputFileId,              // 입력 파일 ID (제품 파일)
        Integer modelFileId,              // 모델 파일 ID
        Integer resultFileId,             // 결과 파일 ID
        String resultFileUrl,             // 결과 파일 URL
        String inputFileUrl,              // 입력 파일 URL (제품 파일)
        String modelFileUrl,              // 모델 파일 URL
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
            null,          // modelFileId
            resultFileId,
            resultFileUrl,
            null,          // inputFileUrl
            null,          // modelFileUrl
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }

    // 성공 응답 생성 (입력 파일 정보 포함)
    public static ComposeResponse success(String jobId, Integer inputFileId, String inputFileUrl, 
                                        Integer resultFileId, String resultFileUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "SUCCEEDED",
            inputFileId,
            null,          // modelFileId
            resultFileId,
            resultFileUrl,
            inputFileUrl,
            null,          // modelFileUrl
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
            null,          // modelFileId
            null,          // resultFileId
            null,          // resultFileUrl
            null,          // inputFileUrl
            null,          // modelFileUrl
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
            null,          // modelFileId
            null,          // resultFileId
            null,          // resultFileUrl
            null,          // inputFileUrl
            null,          // modelFileUrl
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }

    // 처리 중 응답 생성 (입력 파일 정보 포함)
    public static ComposeResponse processing(String jobId, Integer inputFileId, String inputFileUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "PROCESSING",
            inputFileId,
            null,          // modelFileId
            null,          // resultFileId
            null,          // resultFileUrl
            inputFileUrl,
            null,          // modelFileUrl
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }

    // PENDING 상태 응답 생성 (처리 대기 중)
    public static ComposeResponse pending(String jobId, Integer inputFileId, String inputFileUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "PENDING",
            inputFileId,
            null,          // modelFileId
            null,          // resultFileId
            null,          // resultFileUrl
            inputFileUrl,
            null,          // modelFileUrl
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }

    // 처리 중 응답 생성 (제품과 모델 파일 정보 모두 포함)
    public static ComposeResponse processing(String jobId, Integer inputFileId, String inputFileUrl, 
                                           Integer modelFileId, String modelFileUrl) {
        LocalDateTime now = LocalDateTime.now();
        return new ComposeResponse(
            jobId,
            "PROCESSING",
            inputFileId,
            modelFileId,
            null,          // resultFileId
            null,          // resultFileUrl
            inputFileUrl,
            modelFileUrl,
            null,          // errorMessage
            now,           // createdAt
            now            // updatedAt
        );
    }
}
