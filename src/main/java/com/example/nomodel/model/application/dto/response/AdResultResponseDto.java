package com.example.nomodel.model.application.dto.response;

import com.example.nomodel.model.domain.model.AdResult;

import java.time.LocalDateTime;

public record AdResultResponseDto(
    Long id,
    Long modelId,
    Long memberId,
    String prompt,
    String adResultName,
    Double memberRating,
    String resultImageUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    public static AdResultResponseDto from(AdResult adResult) {
        return new AdResultResponseDto(
            adResult.getId(),
            adResult.getModelId(),
            adResult.getMemberId(),
            adResult.getPrompt(),
            adResult.getAdResultName(),
            adResult.getMemberRating(),
            adResult.getResultImageUrl(),
            adResult.getCreatedAt(),
            adResult.getUpdatedAt()
        );
    }
}