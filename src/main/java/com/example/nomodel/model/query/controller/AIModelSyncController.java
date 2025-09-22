package com.example.nomodel.model.query.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.command.application.service.ElasticsearchIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 모델 동기화 API 컨트롤러
 * MySQL과 Elasticsearch 간 데이터 동기화
 */
@RestController
@RequestMapping("/models/sync")
@RequiredArgsConstructor
@Tag(name = "AI Model Sync", description = "AI 모델 동기화 API")
public class AIModelSyncController {

    private final ElasticsearchIndexService indexService;

    @Operation(summary = "AI 모델 인덱스 재생성 및 동기화", 
               description = "Elasticsearch 인덱스를 한글 분석기(Nori) 설정으로 재생성하고 데이터를 동기화")
    @PostMapping("/recreate-index")
    public ResponseEntity<?> recreateIndexAndSync() {
        try {
            // 1. 인덱스 재생성 (한글 분석기 설정 적용)
            indexService.recreateAIModelsIndex();
            
            // 2. 데이터 동기화
            long syncedCount = indexService.syncAllModelsToElasticsearch();
            
            return ResponseEntity.ok(ApiUtils.success(
                String.format("인덱스 재생성 및 동기화 완료 - 성공: %d", syncedCount)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body(ApiUtils.error("재생성 및 동기화 실패: " + e.getMessage()));
        }
    }

}