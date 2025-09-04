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
               description = "Elasticsearch 인덱스를 한글 분석기(Nori) 설정으로 재생성하고 데이터를 동기화")
    @PostMapping("/recreate-index")
    public ResponseEntity<?> recreateIndexAndSync() {
        try {
            log.info("AI 모델 인덱스 재생성 및 동기화 시작");
            
            // 1. 인덱스 재생성 (한글 분석기 설정 적용)
            indexService.recreateAIModelsIndex();
            log.info("인덱스 재생성 완료");
            
            // 2. 데이터 동기화
            long syncedCount = indexService.syncAllModelsToElasticsearch();
            log.info("AI 모델 인덱스 재생성 및 동기화 완료 - 성공: {}", syncedCount);
            
            return ResponseEntity.ok(ApiUtils.success(
                String.format("인덱스 재생성 및 동기화 완료 - 성공: %d", syncedCount)
            ));
            
        } catch (Exception e) {
            log.error("AI 모델 인덱스 재생성 및 동기화 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                   .body(ApiUtils.error("재생성 및 동기화 실패: " + e.getMessage()));
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