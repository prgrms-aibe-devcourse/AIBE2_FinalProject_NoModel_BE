package com.example.nomodel.model.application.dto;

import com.example.nomodel.model.domain.model.OwnType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "모델 카드 정보 응답")
public record ModelCardResponse(
        @Schema(description = "모델 ID", example = "1")
        Long modelId,

        @Schema(description = "모델명", example = "Fantasy Portrait Model v2.1")
        String modelName,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnails/model1.jpg")
        String thumbnailUrl,

        @Schema(description = "소유 타입", example = "ADMIN")
        OwnType ownType,

        @Schema(description = "공개 여부", example = "true")
        boolean isPublic,

        @Schema(description = "고해상도 모델 여부", example = "false")
        boolean isHighResolution,

        @Schema(description = "생성 일시", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시", example = "2024-01-16T14:20:00")
        LocalDateTime updatedAt
) {
    public static ModelCardResponse of(Long modelId, String modelName, String thumbnailUrl,
                                       OwnType ownType, boolean isPublic, boolean isHighResolution,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ModelCardResponse(modelId, modelName, thumbnailUrl, ownType, 
                isPublic, isHighResolution, createdAt, updatedAt);
    }
}