package com.example.nomodel.model.application.dto;

import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 모델 상세 조회 응답 DTO
 */
@Getter
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
    private Long downloadCount;
    private Long viewCount;
    
    // 파일 정보
    private List<FileInfo> files;
    
    // 리뷰 정보
    private List<ReviewResponse> reviews;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class FileInfo {
        private Long fileId;
        private String fileUrl;
        private String fileName;
        private boolean isPrimary;

        public static FileInfo from(File file) {
            return FileInfo.builder()
                    .fileId(file.getId())
                    .fileUrl(file.getFileUrl())
                    .fileName(file.getFileName())
                    .isPrimary(file.isPrimary())
                    .build();
        }
    }

    public static AIModelDetailResponse from(AIModel model, String ownerName, 
                                           AIModelDocument document, List<File> files, 
                                           List<ReviewResponse> reviews) {
        return AIModelDetailResponse.builder()
                .modelId(model.getId())
                .modelName(model.getModelName())
                .description(extractDescription(model))
                .ownType(model.getOwnType().name())
                .ownerName(ownerName)
                .ownerId(model.getOwnerId())
                .price(model.getPrice())
                .avgRating(document != null ? document.getRating() : 0.0)
                .reviewCount(document != null ? document.getReviewCount() : 0L)
                .downloadCount(document != null ? document.getUsageCount() : 0L) // 다운로드를 사용량으로 대체
                .viewCount(document != null ? document.getUsageCount() : 0L) // 임시로 같은 값 사용
                .files(files.stream().map(FileInfo::from).toList())
                .reviews(reviews != null ? reviews : List.of())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    private static String extractDescription(AIModel model) {
        // ModelMetadata에서 prompt를 description으로 사용
        if (model.getModelMetadata() != null && model.getModelMetadata().getPrompt() != null) {
            return model.getModelMetadata().getPrompt();
        }
        return model.getModelName(); // 기본값으로 모델명 사용
    }
}