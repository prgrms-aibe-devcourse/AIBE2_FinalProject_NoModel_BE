package com.example.nomodel.model.application.dto.response;

import com.example.nomodel.model.domain.model.document.AIModelDocument;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 모델 검색 응답 DTO
 * AIModelDocument + File 정보를 조합한 응답
 */
@Getter
@Builder
public class AIModelSearchResponse {

    private String id;
    private Long modelId;
    private String modelName;
    private String prompt;
    private String[] tags;
    private String ownType;
    private Long ownerId;
    private String ownerName;
    private BigDecimal price;
    private Boolean isPublic;
    private Long usageCount;
    private Long viewCount;
    private Double rating;
    private Long reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // File 도메인 정보 (별도 조합)
    private List<String> imageUrls;
    private String primaryImageUrl;

    public static AIModelSearchResponse from(AIModelDocument document, List<String> imageUrls) {
        return AIModelSearchResponse.builder()
                .id(document.getId())
                .modelId(document.getModelId())
                .modelName(document.getModelName())
                .prompt(document.getPrompt())
                .tags(document.getTags())
                .ownType(document.getOwnType())
                .ownerId(document.getOwnerId())
                .ownerName(document.getOwnerName())
                .price(document.getPrice())
                .isPublic(document.getIsPublic())
                .usageCount(document.getUsageCount())
                .viewCount(document.getViewCount())
                .rating(document.getRating())
                .reviewCount(document.getReviewCount())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .imageUrls(imageUrls != null ? imageUrls : List.of())
                .primaryImageUrl(imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null)
                .build();
    }
}