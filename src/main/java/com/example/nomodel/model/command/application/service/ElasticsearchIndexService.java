package com.example.nomodel.model.command.application.service;

import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.command.application.dto.ModelWithStatisticsProjection;
import com.example.nomodel.model.command.domain.model.document.AIModelDocument;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelStatistics;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.command.domain.repository.AIModelSearchRepository;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.review.domain.model.ReviewStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * Elasticsearch 인덱스 관리 서비스
 * 인덱스 생성, 삭제, 설정 적용 등을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexService {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ObjectMapper objectMapper;
    private final AIModelJpaRepository aiModelJpaRepository;
    private final AIModelSearchRepository aiModelSearchRepository;
    private final ModelStatisticsJpaRepository modelStatisticsRepository;
    private final MemberJpaRepository memberRepository;
    private final ReviewRepository reviewRepository;

    /**
     * ai-models 인덱스를 재생성 (settings + mappings 분리)
     */
    public void recreateAIModelsIndex() {
        try {
            IndexOperations indexOps = elasticsearchTemplate.indexOps(AIModelDocument.class);
            
            // 기존 인덱스 삭제
            if (indexOps.exists()) {
                log.info("기존 ai-models 인덱스 삭제 중...");
                indexOps.delete();
            }

            // 1. settings로 인덱스 생성
            log.info("새로운 ai-models 인덱스 생성 중...");
            Document settings = loadSettings();
            indexOps.create(settings);
            
            // 2. mappings 적용
            log.info("인덱스 매핑 적용 중...");
            Document mappings = loadMappings();
            indexOps.putMapping(mappings);
            
            log.info("ai-models 인덱스가 성공적으로 재생성되었습니다.");
            
        } catch (Exception e) {
            log.error("ai-models 인덱스 재생성 중 오류 발생", e);
            throw new RuntimeException("인덱스 재생성 실패", e);
        }
    }

    /**
     * 인덱스 설정 파일 로드
     */
    private Document loadSettings() {
        try {
            ClassPathResource resource = new ClassPathResource("elasticsearch/ai-models-settings.json");
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode jsonNode = objectMapper.readTree(inputStream);
                Map<String, Object> settingsMap = objectMapper.convertValue(jsonNode, Map.class);
                return Document.from(settingsMap);
            }
        } catch (Exception e) {
            log.error("인덱스 설정 파일 로드 실패", e);
            throw new RuntimeException("인덱스 설정 로드 실패", e);
        }
    }

    /**
     * 인덱스 매핑 파일 로드
     */
    private Document loadMappings() {
        try {
            ClassPathResource resource = new ClassPathResource("elasticsearch/ai-models-mappings.json");
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode jsonNode = objectMapper.readTree(inputStream);
                Map<String, Object> mappingsMap = objectMapper.convertValue(jsonNode, Map.class);
                return Document.from(mappingsMap);
            }
        } catch (Exception e) {
            log.error("인덱스 매핑 파일 로드 실패", e);
            throw new RuntimeException("인덱스 매핑 로드 실패", e);
        }
    }

    /**
     * MySQL에서 Elasticsearch로 모든 AI 모델 데이터 동기화
     * 하이브리드 최적화: JOIN 쿼리 + 일괄 리뷰 통계 조회 (2번 쿼리)
     */
    public long syncAllModelsToElasticsearch() {
        try {
            log.info("MySQL → Elasticsearch 전체 AI 모델 동기화 시작 (하이브리드 최적화)");
            
            // 1. JOIN 쿼리로 모델+통계+소유자 한 번에 조회 (1번 쿼리)
            List<ModelWithStatisticsProjection> projections =
                    aiModelJpaRepository.findAllModelsWithStatisticsAndOwner();
            
            log.info("MySQL에서 {} 개의 AI 모델 조회됨 (JOIN 쿼리)", projections.size());
            
            if (projections.isEmpty()) {
                log.info("동기화할 모델이 없습니다.");
                return 0;
            }
            
            // 2. 모든 모델 ID 수집
            List<Long> modelIds = projections.stream()
                    .map(p -> p.getModel().getId())
                    .toList();
            
            // 3. 리뷰 통계만 별도로 일괄 조회 (1번 쿼리)
            Map<Long, Double> ratingMap = buildRatingMap(modelIds);
            Map<Long, Long> reviewCountMap = buildReviewCountMap(modelIds);
            
            log.info("하이브리드 최적화 완료 - 총 2번 쿼리로 모든 데이터 조회");
            
            // 4. Elasticsearch 인덱스 초기화
            aiModelSearchRepository.deleteAll();
            log.info("기존 Elasticsearch 데이터 삭제 완료");
            
            // 5. 각 모델을 Elasticsearch에 색인
            long indexedCount = 0;
            for (ModelWithStatisticsProjection projection : projections) {
                try {
                    AIModel model = projection.getModel();
                    Long modelId = model.getId();
                    
                    // JOIN으로 가져온 데이터 사용
                    String ownerName = projection.getOwnerName() != null ? 
                            projection.getOwnerName() : 
                            (model.getOwnType() != null ? model.getOwnType().name() : "ADMIN");
                    
                    ModelStatistics stats = projection.getStatistics();
                    Long usageCount = stats != null ? stats.getUsageCount() : 0L;
                    Long viewCount = stats != null ? stats.getViewCount() : 0L;
                    
                    // 리뷰 통계는 별도 조회한 것 사용
                    Double rating = ratingMap.getOrDefault(modelId, 0.0);
                    Long reviewCount = reviewCountMap.getOrDefault(modelId, 0L);
                    
                    AIModelDocument document = AIModelDocument.from(
                        model, ownerName, usageCount, viewCount, rating, reviewCount);
                    aiModelSearchRepository.save(document);
                    indexedCount++;
                    
                    if (indexedCount % 50 == 0) {
                        log.info("진행 상황: {}/{} 모델 색인 완료", indexedCount, projections.size());
                    }
                } catch (Exception e) {
                    log.error("모델 색인 실패: modelId={}, error={}", 
                            projection.getModel().getId(), e.getMessage());
                }
            }
            
            log.info("MySQL → Elasticsearch 동기화 완료: {} 개 모델 색인됨 (하이브리드 최적화)", indexedCount);
            return indexedCount;
            
        } catch (Exception e) {
            log.error("AI 모델 동기화 중 오류 발생", e);
            throw new RuntimeException("동기화 실패", e);
        }
    }
    
    /**
     * 소유자 이름 맵 생성 (일괄 최적화)
     */
    private Map<Long, String> buildOwnerNameMap(List<AIModel> models) {
        Map<Long, String> ownerNameMap = new HashMap<>();
        
        // 소유자가 있는 모델들의 소유자 ID 수집
        List<Long> ownerIds = models.stream()
                .filter(model -> model.getOwnerId() != null)
                .map(AIModel::getOwnerId)
                .distinct()
                .toList();
        
        // 일괄 소유자 조회
        Map<Long, String> memberEmailMap = memberRepository.findAllById(ownerIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, member -> member.getEmail().getValue()));
        
        // 각 모델의 소유자 이름 매핑
        for (AIModel model : models) {
            String ownerName;
            if (model.getOwnerId() == null) {
                ownerName = model.getOwnType() != null ? model.getOwnType().name() : "ADMIN";
            } else {
                ownerName = memberEmailMap.getOrDefault(model.getOwnerId(), "Unknown");
            }
            ownerNameMap.put(model.getId(), ownerName);
        }
        
        return ownerNameMap;
    }
    
    /**
     * 평점 맵 생성 (일괄 최적화)
     */
    private Map<Long, Double> buildRatingMap(List<Long> modelIds) {
        List<Object[]> ratingResults = reviewRepository.getReviewStatisticsByModelIds(modelIds, ReviewStatus.ACTIVE);
        
        Map<Long, Double> ratingMap = new HashMap<>();
        for (Object[] result : ratingResults) {
            Long modelId = (Long) result[0];
            Double avgRating = (Double) result[2];
            ratingMap.put(modelId, avgRating != null ? avgRating : 0.0);
        }
        
        return ratingMap;
    }
    
    /**
     * 리뷰 개수 맵 생성 (일괄 최적화)
     */
    private Map<Long, Long> buildReviewCountMap(List<Long> modelIds) {
        List<Object[]> reviewResults = reviewRepository.getReviewStatisticsByModelIds(modelIds, ReviewStatus.ACTIVE);
        
        Map<Long, Long> reviewCountMap = new HashMap<>();
        for (Object[] result : reviewResults) {
            Long modelId = (Long) result[0];
            Long reviewCount = (Long) result[1];
            reviewCountMap.put(modelId, reviewCount != null ? reviewCount : 0L);
        }
        
        return reviewCountMap;
    }

    /**
     * 소유자 이름 조회 (개별 - 사용 안함)
     */
    @Deprecated
    private String getOwnerName(AIModel aiModel) {
        if (aiModel.getOwnerId() == null) {
            return aiModel.getOwnType() != null ? aiModel.getOwnType().name() : "ADMIN";
        }
        
        return memberRepository.findById(aiModel.getOwnerId())
                .map(Member::getEmail)
                .map(Email::getValue)
                .orElse("Unknown");
    }

    /**
     * 모델의 사용량 조회 (개별 - 사용 안함)
     */
    @Deprecated
    private Long getUsageCount(AIModel aiModel) {
        return modelStatisticsRepository.findByModelId(aiModel.getId())
                .map(ModelStatistics::getUsageCount)
                .orElse(0L);
    }

    /**
     * 모델의 조회수 조회 (개별 - 사용 안함)
     */
    @Deprecated
    private Long getViewCount(AIModel aiModel) {
        return modelStatisticsRepository.findByModelId(aiModel.getId())
                .map(ModelStatistics::getViewCount)
                .orElse(0L);
    }

    /**
     * 모델의 평점 조회 (개별 - 사용 안함)
     */
    @Deprecated
    private Double getAverageRating(AIModel aiModel) {
        return reviewRepository.calculateAverageRatingByModelId(aiModel.getId(), ReviewStatus.ACTIVE);
    }

    /**
     * 모델의 리뷰 수 조회 (개별 - 사용 안함)
     */
    @Deprecated
    private Long getReviewCount(AIModel aiModel) {
        return reviewRepository.countByModelIdAndStatus(aiModel.getId(), ReviewStatus.ACTIVE);
    }
    
    /**
     * 인덱스 상태 확인
     */
    public boolean isIndexHealthy() {
        try {
            IndexOperations indexOps = elasticsearchTemplate.indexOps(AIModelDocument.class);
            return indexOps.exists();
        } catch (Exception e) {
            log.warn("인덱스 상태 확인 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 인덱스 통계 정보 조회
     */
    public Map<String, Object> getIndexStats() {
        try {
            long totalDocuments = aiModelSearchRepository.count();
            boolean indexExists = isIndexHealthy();
            
            return Map.of(
                "indexExists", indexExists,
                "totalDocuments", totalDocuments,
                "indexName", "ai-models"
            );
        } catch (Exception e) {
            log.error("인덱스 통계 조회 실패", e);
            return Map.of("error", e.getMessage());
        }
    }
}