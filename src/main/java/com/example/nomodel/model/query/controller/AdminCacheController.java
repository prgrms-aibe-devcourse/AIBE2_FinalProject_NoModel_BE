package com.example.nomodel.model.query.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.command.application.dto.response.cache.CacheStatusResponse;
import com.example.nomodel.model.command.application.cache.service.SmartCacheEvictionService;
import com.example.nomodel.model.command.application.cache.service.LazyInvalidationService;
import com.example.nomodel.model.command.application.cache.service.ModelCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 관리자용 긴급 캐시 관리 API
 * 긴급 상황, 배포 직후, 디버깅 전용
 * 일반적인 캐시 관리는 자동화됨
 */
@Slf4j
@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
@Tag(name = "Emergency Cache Management", description = "긴급 캐시 관리 API (자동화된 시스템 보완용)")
public class AdminCacheController {

    private final SmartCacheEvictionService smartCacheEvictionService;
    private final LazyInvalidationService lazyInvalidationService;
    private final ModelCacheService modelCacheService;

    @Operation(summary = "긴급 캐시 무효화", description = "잘못된 데이터 캐싱 시 즉시 제거")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "긴급 무효화 완료"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/emergency/{modelId}")
    public ResponseEntity<?> emergencyEviction(
            @Parameter(description = "문제가 된 모델 ID") @PathVariable Long modelId,
            @Parameter(description = "긴급 사유") @RequestParam String reason) {

        log.error("긴급 캐시 무효화 요청: modelId={}, reason={}", modelId, reason);
        smartCacheEvictionService.emergencyEviction(modelId, reason);

        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @Operation(summary = "디버깅용 캐시 상태 조회", description = "현재 캐시 상태와 dirty 상태 확인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/debug/status")
    public ResponseEntity<?> getDebugStatus() {
        log.info("디버깅용 캐시 상태 조회");

        // 각 서비스의 상태 정보 수집
        CacheStatusResponse cacheStatus = new CacheStatusResponse(
                LocalDateTime.now(),
                smartCacheEvictionService.getCacheStatus(),
                lazyInvalidationService.getStatus(),
                modelCacheService.getCacheStatus()
        );

        return ResponseEntity.ok(ApiUtils.success(cacheStatus));
    }

    @Operation(summary = "배치 처리 강제 실행", description = "대량 변경 후 즉시 배치 처리")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배치 처리 완료"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/batch/process")
    public ResponseEntity<?> forceBatchProcess() {
        log.warn("배치 처리 강제 실행 요청");
        lazyInvalidationService.processAllDirtyImmediately();

        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @Operation(summary = "캐시 검증 및 복구", description = "데이터 불일치 검사 및 자동 복구")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 완료"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/validate/{modelId}")
    public ResponseEntity<?> validateAndRepairCache(
            @Parameter(description = "검증할 모델 ID") @PathVariable Long modelId) {

        log.info("캐시 검증 및 복구 요청: modelId={}", modelId);
        modelCacheService.validateAndRepairCache(modelId);

        return ResponseEntity.ok(ApiUtils.success(null));
    }
}