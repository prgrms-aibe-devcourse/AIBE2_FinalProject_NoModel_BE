package com.example.nomodel.model.application.service;

import com.example.nomodel.model.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.application.dto.cache.ModelCacheStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 모델 캐시 관리 서비스
 * Cache-Aside 패턴으로 캐시 갱신 및 무효화 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCacheService {

    private final AIModelDetailService modelDetailService;
    private final CacheManager cacheManager;

    /**
     * 모델 상세 캐시 즉시 갱신
     * Cache-Aside 패턴으로 DB 조회 후 캐시 갱신
     */
    @Transactional(readOnly = true)
    public void updateModelDetailCache(Long modelId) {
        log.debug("모델 상세 캐시 갱신: modelId={}", modelId);

        // 1. 최신 데이터 조회
        AIModelDetailResponse freshData = modelDetailService.getModelDetail(modelId);

        // 2. 캐시에 즉시 업데이트
        Cache cache = cacheManager.getCache("modelDetail");
        if (cache != null) {
            cache.put(modelId, freshData);
            log.debug("모델 상세 캐시 갱신 완료: modelId={}", modelId);
        }
    }

    /**
     * 캐시 검증 및 복구
     * 데이터 불일치 발견 시 자동 복구
     */
    public void validateAndRepairCache(Long modelId) {
        Cache cache = cacheManager.getCache("modelDetail");
        if (cache == null) {
            return;
        }

        // 캐시된 데이터와 DB 데이터 비교
        Cache.ValueWrapper cachedValue = cache.get(modelId);
        if (cachedValue == null) {
            // 캐시 없음 - 정상
            return;
        }

        AIModelDetailResponse freshData = modelDetailService.getModelDetail(modelId);
        AIModelDetailResponse cachedData = (AIModelDetailResponse) cachedValue.get();

        // 간단한 버전 체크 (수정 시간 비교)
        if (freshData != null && cachedData != null) {
            if (!freshData.getUpdatedAt().equals(cachedData.getUpdatedAt())) {
                log.warn("캐시 불일치 감지 및 복구: modelId={}", modelId);
                cache.put(modelId, freshData);
            }
        }

    }

    /**
     * 모델 캐시 서비스 상태 정보 반환
     */
    public ModelCacheStatusResponse getCacheStatus() {
        Cache cache = cacheManager.getCache("modelDetail");
        String cacheImplementation = "N/A";

        if (cache != null) {
            cacheImplementation = cache.getNativeCache().getClass().getSimpleName();
        }

        return new ModelCacheStatusResponse(
                "ModelCacheService",
                cacheImplementation,
                LocalDateTime.now()
        );
    }
}