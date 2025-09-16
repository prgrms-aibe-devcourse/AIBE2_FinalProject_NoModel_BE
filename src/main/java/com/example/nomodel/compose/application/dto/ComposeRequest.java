package com.example.nomodel.compose.application.dto;

import jakarta.validation.constraints.NotNull;

public record ComposeRequest(
        @NotNull(message = "Product image file ID is required")
        Integer productFileId,        // 제품 이미지 파일 ID
        
        @NotNull(message = "Model image file ID is required")
        Integer modelFileId,        // 모델 이미지 파일 ID
        
        String customPrompt      // 사용자 정의 프롬프트 (선택사항)
) {
    public ComposeRequest {
        // 커스텀 프롬프트가 빈 문자열이면 null로 처리
        if (customPrompt != null && customPrompt.trim().isEmpty()) {
            customPrompt = null;
        }
    }
}
