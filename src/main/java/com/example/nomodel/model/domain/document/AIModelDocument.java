package com.example.nomodel.model.domain.document;

import com.example.nomodel.model.domain.model.AIModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AIModel Elasticsearch 문서
 * AI 모델 검색을 위한 검색 엔진 문서
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "ai-models")
@Setting(settingPath = "/elasticsearch/ai-models-settings.json")
@Mapping(mappingPath = "/elasticsearch/ai-models-mappings.json")
public class AIModelDocument {

    @Id
    private String id;

    /**
     * 원본 AIModel의 데이터베이스 ID
     */
    @Field(type = FieldType.Long)
    private Long modelId;

    /**
     * 모델명 (한국어 분석기 적용)
     */
    @Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_search_analyzer")
    private String modelName;

    /**
     * 자동완성을 위한 모델명 (completion suggester)
     */
    @CompletionField(maxInputLength = 100)
    private String suggest;

    /**
     * 부분 검색을 위한 모델명 (edge n-gram)
     */
    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "nori_search_analyzer")
    private String modelNameEdgeNgram;

    /**
     * 모델 프롬프트 (검색용)
     */
    @Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_search_analyzer")
    private String prompt;

    /**
     * 모델 태그들 (검색용)
     */
    @Field(type = FieldType.Keyword)
    private String[] tags;

    /**
     * 소유 타입 (USER, ADMIN)
     */
    @Field(type = FieldType.Keyword)
    private String ownType;

    /**
     * 소유자 ID
     */
    @Field(type = FieldType.Long)
    private Long ownerId;

    /**
     * 소유자 이름 (검색용)
     */
    @Field(type = FieldType.Keyword)
    private String ownerName;

    /**
     * 가격
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 공개 여부
     */
    @Field(type = FieldType.Boolean)
    private Boolean isPublic;


    /**
     * 사용량/인기도
     */
    @Field(type = FieldType.Long)
    private Long usageCount;

    /**
     * 평점
     */
    @Field(type = FieldType.Double)
    private Double rating;

    /**
     * 리뷰 수
     */
    @Field(type = FieldType.Long)
    private Long reviewCount;

    /**
     * 생성일
     */
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    /**
     * 수정일
     */
    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    @Builder
    private AIModelDocument(Long modelId, String modelName, String prompt,
                           String[] tags, String ownType, Long ownerId, String ownerName,
                           BigDecimal price, Boolean isPublic,
                           Long usageCount, Double rating, Long reviewCount,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.suggest = modelName; // 자동완성용 (모델명과 동일)
        this.modelNameEdgeNgram = modelName; // 부분 검색용 (모델명과 동일)
        this.prompt = prompt;
        this.tags = tags;
        this.ownType = ownType;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.price = price;
        this.isPublic = isPublic;
        this.usageCount = usageCount != null ? usageCount : 0L;
        this.rating = rating != null ? rating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0L;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * AIModel 엔티티로부터 AIModelDocument 생성
     */
    public static AIModelDocument from(AIModel aiModel, String ownerName) {
        return AIModelDocument.builder()
                .modelId(aiModel.getId())
                .modelName(aiModel.getModelName())
                .prompt(extractPrompt(aiModel))
                .tags(extractTags(aiModel))
                .ownType(aiModel.getOwnType().name())
                .ownerId(aiModel.getOwnerId())
                .ownerName(ownerName)
                .price(aiModel.getPrice())
                .isPublic(aiModel.isPublic())
                .createdAt(aiModel.getCreatedAt())
                .updatedAt(aiModel.getUpdatedAt())
                .build();
    }

    /**
     * 사용량 증가
     */
    public void increaseUsage() {
        this.usageCount = this.usageCount != null ? this.usageCount + 1 : 1L;
    }

    /**
     * 평점 업데이트
     */
    public void updateRating(Double rating, Long reviewCount) {
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    /**
     * 공개 상태 변경
     */
    public void updateVisibility(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * 가격 업데이트
     */
    public void updatePrice(BigDecimal price) {
        this.price = price;
    }


    // Private helper methods for extraction
    private static String extractPrompt(AIModel aiModel) {
        // ModelMetadata에서 prompt 추출
        if (aiModel.getModelMetadata() != null && aiModel.getModelMetadata().getPrompt() != null) {
            return aiModel.getModelMetadata().getPrompt();
        }
        return "";
    }

    private static String[] extractTags(AIModel aiModel) {
        // ModelMetadata의 sampler 정보를 태그로 활용
        if (aiModel.getModelMetadata() != null && aiModel.getModelMetadata().getSamplerIndex() != null) {
            return new String[]{"AI", "IMAGE_GENERATION", aiModel.getModelMetadata().getSamplerIndex().name()};
        }
        return new String[]{"AI", "IMAGE_GENERATION"};
    }
}