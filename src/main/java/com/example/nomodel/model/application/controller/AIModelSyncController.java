package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.model.application.service.ElasticsearchIndexService;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 모델 동기화 API 컨트롤러
 * MySQL과 Elasticsearch 간 데이터 동기화
 */
@Slf4j
@RestController
@RequestMapping("/models/sync")
@RequiredArgsConstructor
@Tag(name = "AI Model Sync", description = "AI 모델 동기화 API")
public class AIModelSyncController {

    private final AIModelJpaRepository aiModelRepository;
    private final MemberJpaRepository memberRepository;
    private final AIModelSearchService searchService;
    private final ElasticsearchIndexService indexService;

    @Operation(summary = "AI 모델 인덱스 재생성 및 동기화", 
               description = "Elasticsearch 인덱스를 최신 설정(자동완성 지원)으로 재생성하고 데이터를 동기화")
    @PostMapping("/recreate-index")
    public ResponseEntity<?> recreateIndexAndSync() {
        try {
            log.info("AI 모델 인덱스 재생성 및 동기화 시작");
            
            // 1. 인덱스 재생성 (최신 설정 적용)
            indexService.recreateAIModelsIndex();
            log.info("인덱스 재생성 완료");
            
            // 2. 데이터 동기화
            List<AIModel> allModels = aiModelRepository.findAll();
            log.info("동기화할 AI 모델 수: {}", allModels.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (AIModel model : allModels) {
                try {
                    String ownerName = getOwnerName(model.getOwnerId());
                    searchService.indexModel(model, ownerName);
                    successCount++;
                    
                    if (successCount % 100 == 0) {
                        log.info("진행 상황: {}/{} 완료", successCount, allModels.size());
                    }
                    
                } catch (Exception e) {
                    log.error("AI 모델 인덱싱 실패: modelId={}, error={}", 
                             model.getId(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            log.info("AI 모델 인덱스 재생성 및 동기화 완료 - 성공: {}, 실패: {}", 
                     successCount, failureCount);
            
            return ResponseEntity.ok(ApiUtils.success(
                String.format("인덱스 재생성 및 동기화 완료 - 성공: %d, 실패: %d", successCount, failureCount)
            ));
            
        } catch (Exception e) {
            log.error("AI 모델 인덱스 재생성 및 동기화 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                   .body(ApiUtils.error("재생성 및 동기화 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "AI 모델 Elasticsearch 동기화", 
               description = "MySQL의 모든 AI 모델 데이터를 Elasticsearch에 동기화")
    @PostMapping("/elasticsearch")
    public ResponseEntity<?> syncToElasticsearch() {
        try {
            log.info("AI 모델 Elasticsearch 동기화 시작");
            
            // 모든 AI 모델 조회
            List<AIModel> allModels = aiModelRepository.findAll();
            log.info("동기화할 AI 모델 수: {}", allModels.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (AIModel model : allModels) {
                try {
                    // 소유자 정보 조회
                    String ownerName = getOwnerName(model.getOwnerId());
                    
                    // Elasticsearch에 인덱싱
                    searchService.indexModel(model, ownerName);
                    successCount++;
                    
                    if (successCount % 100 == 0) {
                        log.info("진행 상황: {}/{} 완료", successCount, allModels.size());
                    }
                    
                } catch (Exception e) {
                    log.error("AI 모델 인덱싱 실패: modelId={}, error={}", 
                             model.getId(), e.getMessage(), e);
                    failureCount++;
                }
            }
            
            log.info("AI 모델 Elasticsearch 동기화 완료 - 성공: {}, 실패: {}", 
                     successCount, failureCount);
            
            return ResponseEntity.ok(ApiUtils.success(
                String.format("동기화 완료 - 성공: %d, 실패: %d", successCount, failureCount)
            ));
            
        } catch (Exception e) {
            log.error("AI 모델 Elasticsearch 동기화 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                   .body(ApiUtils.error("동기화 실패: " + e.getMessage()));
        }
    }
    
    private String getOwnerName(Long ownerId) {
        if (ownerId == null) {
            return "ADMIN";
        }
        
        return memberRepository.findById(ownerId)
                .map(Member::getEmail)
                .map(email -> email.getValue())
                .orElse("Unknown");
    }
}