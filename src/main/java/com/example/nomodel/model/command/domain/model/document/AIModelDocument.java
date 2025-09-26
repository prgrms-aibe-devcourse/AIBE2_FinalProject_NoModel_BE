package com.example.nomodel.model.command.domain.model.document;

import com.example.nomodel.model.command.domain.model.AIModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
    private Long modelId;

    /**
     * 모델명 (한국어 분석기 적용)
     */
    private String modelName;

    /**
     * 자동완성을 위한 모델명 (completion suggester)
     * 다중 입력 방식 지원: 전체 모델명 + 개별 단어들
     */
    private java.util.List<String> suggest;

    /**
     * 모델 프롬프트 (검색용)
     */
    private String prompt;

    /**
     * 모델 태그들 (검색용)
     */
    private String[] tags;

    /**
     * 소유 타입 (USER, ADMIN)
     */
    private String ownType;

    /**
     * 소유자 ID
     */
    private Long ownerId;

    /**
     * 소유자 이름 (검색용)
     */
    private String ownerName;

    /**
     * 가격
     */
    private BigDecimal price;

    /**
     * 공개 여부
     */
    private Boolean isPublic;

    /**
     * 사용량/인기도
     */
    private Long usageCount;

    /**
     * 조회수
     */
    private Long viewCount;

    /**
     * 평점
     */
    private Double rating;

    /**
     * 리뷰 수
     */
    private Long reviewCount;

    /**
     * 생성일
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    /**
     * 수정일
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updatedAt;

    @Builder
    private AIModelDocument(Long modelId, String modelName, java.util.List<String> suggest, String prompt,
                           String[] tags, String ownType, Long ownerId, String ownerName,
                           BigDecimal price, Boolean isPublic,
                           Long usageCount, Long viewCount, Double rating, Long reviewCount,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.suggest = suggest != null ? suggest : buildSuggestions(modelName); // 자동완성용 (다중 입력 방식)
        this.prompt = prompt;
        this.tags = tags;
        this.ownType = ownType;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.price = price;
        this.isPublic = isPublic;
        this.usageCount = usageCount != null ? usageCount : 0L;
        this.viewCount = viewCount != null ? viewCount : 0L;
        this.rating = rating != null ? rating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0L;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * AIModel 엔티티와 모든 통계 정보로부터 완전한 AIModelDocument 생성
     * 
     * @param aiModel AI 모델 엔티티
     * @param ownerName 소유자 이름
     * @param usageCount 사용량 (null이면 0)
     * @param viewCount 조회수 (null이면 0) 
     * @param rating 평점 (null이면 0.0)
     * @param reviewCount 리뷰 수 (null이면 0)
     */
    public static AIModelDocument from(AIModel aiModel, String ownerName, 
                                     Long usageCount, Long viewCount, 
                                     Double rating, Long reviewCount) {

        AIModelDocument aiModelDocument = AIModelDocument.builder()
                .modelId(aiModel.getId())
                .modelName(aiModel.getModelName())
                .suggest(buildSuggestions(aiModel.getModelName()))
                .prompt(extractPrompt(aiModel))
                .tags(extractTags(aiModel))
                .ownType(aiModel.getOwnType().name())
                .ownerId(aiModel.getOwnerId())
                .ownerName(ownerName)
                .price(aiModel.getPrice())
                .isPublic(aiModel.isPublic())
                .usageCount(usageCount != null ? usageCount : 0L)
                .viewCount(viewCount != null ? viewCount : 0L)
                .rating(rating != null ? rating : 0.0)
                .reviewCount(reviewCount != null ? reviewCount : 0L)
                .createdAt(aiModel.getCreatedAt())
                .updatedAt(aiModel.getUpdatedAt())
                .build();

        aiModelDocument.id = String.valueOf(aiModel.getId());

        return aiModelDocument;
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

    /**
     * 모델명으로부터 다양한 자동완성 제안 생성
     * 예: "Stable Diffusion v1.5" → ["Stable Diffusion v1.5", "stable", "diffusion"]
     */
    private static java.util.List<String> buildSuggestions(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 1. 공백과 특수문자로 단어 분리
        String[] words = modelName.trim()
                .toLowerCase()  // 소문자로 통일
                .split("[\\s\\-\\_\\.]"); // 공백, 하이픈, 언더스코어, 점으로 분리
        
        // 2. 유효한 단어들만 추가 (2글자 이상)
        java.util.List<String> validWords = new java.util.ArrayList<>();
        for (String word : words) {
            String trimmedWord = word.trim();
            if (trimmedWord.length() >= 2 && !trimmedWord.matches("^[0-9.]+$")) { // 숫자만으로 된 단어 제외
                validWords.add(trimmedWord);
            }
        }
        
        // 3. 전체 모델명 + 개별 단어들 결합
        java.util.List<String> allSuggestions = new java.util.ArrayList<>();
        allSuggestions.add(modelName.trim()); // 전체 모델명
        allSuggestions.addAll(validWords); // 개별 단어들
        
        return allSuggestions;
    }
}