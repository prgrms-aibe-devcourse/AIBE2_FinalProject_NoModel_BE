package com.example.nomodel.model.application.service;

import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
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
     */
    public long syncAllModelsToElasticsearch() {
        try {
            log.info("MySQL → Elasticsearch 전체 AI 모델 동기화 시작");
            
            // 1. MySQL에서 모든 AI 모델 조회
            List<AIModel> allModels = aiModelJpaRepository.findAll();
            log.info("MySQL에서 {} 개의 AI 모델 조회됨", allModels.size());
            
            // 2. Elasticsearch 인덱스 초기화
            aiModelSearchRepository.deleteAll();
            log.info("기존 Elasticsearch 데이터 삭제 완료");
            
            // 3. 각 모델을 Elasticsearch에 색인
            long indexedCount = 0;
            for (AIModel model : allModels) {
                try {
                    String ownerName = getOwnerName(model);
                    AIModelDocument document = AIModelDocument.from(model, ownerName);
                    aiModelSearchRepository.save(document);
                    indexedCount++;
                    
                    if (indexedCount % 50 == 0) {
                        log.info("진행 상황: {}/{} 모델 색인 완료", indexedCount, allModels.size());
                    }
                } catch (Exception e) {
                    log.error("모델 색인 실패: modelId={}, error={}", model.getId(), e.getMessage());
                }
            }
            
            log.info("MySQL → Elasticsearch 동기화 완료: {} 개 모델 색인됨", indexedCount);
            return indexedCount;
            
        } catch (Exception e) {
            log.error("AI 모델 동기화 중 오류 발생", e);
            throw new RuntimeException("동기화 실패", e);
        }
    }
    
    /**
     * 소유자 이름 조회 (임시 구현 - Member 서비스 연동 필요)
     */
    private String getOwnerName(AIModel model) {
        // TODO: MemberService에서 실제 소유자 이름을 조회해야 함
        if (model.getOwnType() != null) {
            return model.getOwnType().name() + "_USER_" + model.getOwnerId();
        }
        return "UNKNOWN_USER_" + model.getOwnerId();
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