package com.example.nomodel.model.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "모델 갤러리 응답")
public record ModelGalleryResponse(
        @Schema(description = "모델 카드 정보 목록")
        List<ModelCardResponse> models,

        @Schema(description = "총 개수", example = "15")
        int totalCount,

        @Schema(description = "관리자 모델 개수", example = "10")
        long adminModelCount,

        @Schema(description = "사용자 모델 개수", example = "5")
        long userModelCount
) {
    public static ModelGalleryResponse of(List<ModelCardResponse> models) {
        long adminCount = models.stream()
                .filter(model -> model.ownType().isAdminOwned())
                .count();
        long userCount = models.stream()
                .filter(model -> model.ownType().isUserOwned())
                .count();

        return new ModelGalleryResponse(models, models.size(), adminCount, userCount);
    }

    public static ModelGalleryResponse empty() {
        return new ModelGalleryResponse(List.of(), 0, 0L, 0L);
    }
}