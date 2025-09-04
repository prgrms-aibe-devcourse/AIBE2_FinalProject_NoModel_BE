package com.example.nomodel.model.application.service;

import com.example.nomodel.model.domain.document.AIModelDocument;
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

    /**
     * ai-models 인덱스를 최신 설정으로 재생성
     */
    public void recreateAIModelsIndex() {
        recreateAIModelsIndex(false);
    }

    /**
     * ai-models 인덱스를 한글 분석기 포함하여 재생성
     */
    public void recreateAIModelsIndexWithKorean() {
        recreateAIModelsIndex(true);
    }

    /**
     * ai-models 인덱스를 설정에 따라 재생성
     */
    public void recreateAIModelsIndex(boolean useKoreanAnalyzer) {
        try {
            IndexOperations indexOps = elasticsearchTemplate.indexOps(AIModelDocument.class);
            
            // 기존 인덱스 삭제
            if (indexOps.exists()) {
                log.info("기존 ai-models 인덱스 삭제 중...");
                indexOps.delete();
            }

            // 새로운 인덱스 생성 (설정 파일 사용)
            log.info("새로운 ai-models 인덱스 생성 중... (한글 분석기: {})", useKoreanAnalyzer);
            Document settings = loadIndexSettings(useKoreanAnalyzer);
            indexOps.create(settings);
            
            log.info("ai-models 인덱스가 성공적으로 재생성되었습니다.");
            
        } catch (Exception e) {
            log.error("ai-models 인덱스 재생성 중 오류 발생", e);
            throw new RuntimeException("인덱스 재생성 실패", e);
        }
    }

    /**
     * 인덱스 설정 파일 로드
     */
    private Document loadIndexSettings() {
        return loadIndexSettings(false);
    }

    /**
     * 인덱스 설정 파일 로드 (한글 분석기 옵션)
     */
    private Document loadIndexSettings(boolean useKoreanAnalyzer) {
        try {
            String settingsFile = useKoreanAnalyzer 
                ? "elasticsearch/ai-models-index-settings-korean.json"
                : "elasticsearch/ai-models-index-settings.json";
                
            ClassPathResource resource = new ClassPathResource(settingsFile);
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
}