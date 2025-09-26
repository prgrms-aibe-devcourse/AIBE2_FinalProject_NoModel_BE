package com.example.nomodel.model.command.application.dto;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.model.command.application.dto.response.AIModelDynamicStats;
import com.example.nomodel.model.command.application.dto.response.AIModelStaticDetail;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIModelDetailResponse {

    private Long modelId;
    private String modelName;
    private String description;
    private String ownType;
    private String ownerName;
    private Long ownerId;
    private BigDecimal price;
    private Double avgRating;
    private Long reviewCount;
    private Long usageCount;
    private Long viewCount;
    private List<FileInfo> files;
    private List<ReviewResponse> reviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private Long fileId;
        private String fileUrl;
        private String fileName;

        @JsonProperty("isPrimary")
        private boolean primary;

        public static FileInfo from(File file) {
            return FileInfo.builder()
                    .fileId(file.getId())
                    .fileUrl(file.getFileUrl())
                    .fileName(file.getFileName())
                    .primary(file.isPrimary())
                    .build();
        }
    }

    public static AIModelDetailResponse of(AIModelStaticDetail staticDetail,
                                           AIModelDynamicStats dynamicStats,
                                           List<ReviewResponse> reviews) {
        return AIModelDetailResponse.builder()
                .modelId(staticDetail.getModelId())
                .modelName(staticDetail.getModelName())
                .description(staticDetail.getDescription())
                .ownType(staticDetail.getOwnType())
                .ownerName(staticDetail.getOwnerName())
                .ownerId(staticDetail.getOwnerId())
                .price(staticDetail.getPrice())
                .files(staticDetail.getFiles())
                .createdAt(staticDetail.getCreatedAt())
                .updatedAt(staticDetail.getUpdatedAt())
                .avgRating(dynamicStats.getAvgRating())
                .reviewCount(dynamicStats.getReviewCount())
                .usageCount(dynamicStats.getUsageCount())
                .viewCount(dynamicStats.getViewCount())
                .reviews(reviews != null ? reviews : List.of())
                .build();
    }
}