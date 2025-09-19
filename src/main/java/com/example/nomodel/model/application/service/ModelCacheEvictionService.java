package com.example.nomodel.model.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 모델 검색 캐시 무효화 서비스
 * 데이터 변경 시 관련 캐시를 선택적으로 무효화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCacheEvictionService {

    private final CacheManager cacheManager;

    /**
     * 모델 생성 시 캐시 무효화
     * 최신 모델, 전체 검색 캐시 무효화
     */
    @Caching(evict = {
            @CacheEvict(value = "recentModels", allEntries = true),
            @CacheEvict(value = "modelSearch", condition = "#isPublic == true", allEntries = true)
    })
    public void evictOnModelCreate(boolean isPublic) {
        log.info("모델 생성으로 인한 캐시 무효화: isPublic={}", isPublic);
    }

    /**
     * 모델 업데이트 시 캐시 무효화
     * 관련된 특정 캐시만 선택적으로 무효화
     */
    @Caching(evict = {
            @CacheEvict(value = "modelDetail", key = "#modelId"),
            @CacheEvict(value = "modelSearch", allEntries = true)
    })
    public void evictOnModelUpdate(Long modelId) {
        log.info("모델 업데이트로 인한 캐시 무효화: modelId={}", modelId);
    }

    /**
     * 모델 삭제 시 캐시 무효화
     */
    @Caching(evict = {
            @CacheEvict(value = "modelDetail", key = "#modelId"),
            @CacheEvict(value = "modelSearch", allEntries = true),
            @CacheEvict(value = "recentModels", allEntries = true)
    })
    public void evictOnModelDelete(Long modelId) {
        log.info("모델 삭제로 인한 캐시 무효화: modelId={}", modelId);
    }

    /**
     * 평점 변경 시 캐시 무효화
     * 인기 모델, 추천 모델 캐시 무효화
     */
    @Caching(evict = {
            @CacheEvict(value = "popularModels", allEntries = true),
            @CacheEvict(value = "recommendedModels", allEntries = true),
            @CacheEvict(value = "modelDetail", key = "#modelId")
    })
    public void evictOnRatingChange(Long modelId) {
        log.info("평점 변경으로 인한 캐시 무효화: modelId={}", modelId);
    }

    /**
     * 가격 변경 시 캐시 무효화
     * 무료 모델 캐시 포함
     */
    @Caching(evict = {
            @CacheEvict(value = "freeModels", allEntries = true),
            @CacheEvict(value = "modelSearch", allEntries = true),
            @CacheEvict(value = "modelDetail", key = "#modelId")
    })
    public void evictOnPriceChange(Long modelId, boolean wasFreeBefore, boolean isFreeNow) {
        log.info("가격 변경으로 인한 캐시 무효화: modelId={}, wasFreeBefore={}, isFreeNow={}",
                modelId, wasFreeBefore, isFreeNow);
    }

    /**
     * 관리자 모델 변경 시 캐시 무효화
     */
    @CacheEvict(value = "adminModels", allEntries = true)
    public void evictAdminModelsCache() {
        log.info("관리자 모델 캐시 무효화");
    }

    /**
     * 전체 검색 캐시 무효화
     * 대규모 변경이나 배치 작업 후 사용
     */
    public void evictAllSearchCaches() {
        List<String> cacheNames = Arrays.asList(
                "modelSearch",
                "popularModels",
                "recentModels",
                "recommendedModels",
                "adminModels",
                "freeModels",
                "autoComplete"
        );

        cacheNames.forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("캐시 전체 삭제: {}", cacheName);
            }
        });
    }

    /**
     * 특정 캐시의 특정 키만 무효화
     */
    public void evictSpecificCacheKey(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("특정 캐시 키 무효화: cache={}, key={}", cacheName, key);
        }
    }

    /**
     * 비동기 캐시 무효화
     * 대량 작업 시 성능 영향 최소화
     */
    @Async
    public void evictCachesAsync(List<String> cacheNames) {
        cacheNames.forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("비동기 캐시 무효화: {}", cacheName);
            }
        });
    }

    /**
     * 스케줄된 캐시 정리
     * 매일 새벽 3시에 오래된 캐시 정리
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledCacheEviction() {
        log.info("스케줄된 캐시 정리 시작");

        // 자주 변경되는 캐시들 정리
        evictSpecificCaches(Arrays.asList("recentModels", "modelSearch"));

        // 통계 캐시 갱신을 위한 정리
        evictSpecificCaches(Arrays.asList("popularModels", "recommendedModels"));

        log.info("스케줄된 캐시 정리 완료");
    }

    /**
     * 특정 캐시들만 정리
     */
    private void evictSpecificCaches(List<String> cacheNames) {
        cacheNames.stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(cache -> {
                    cache.clear();
                    log.debug("캐시 정리: {}", cache.getName());
                });
    }

    /**
     * 캐시 통계 조회
     * 캐시 사용 현황 모니터링용
     */
    public void logCacheStatistics() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // 실제 구현은 Redis 클라이언트를 통해 상세 통계 조회 가능
                log.info("캐시 상태: name={}", cacheName);
            }
        });
    }
}