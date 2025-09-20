package com.example.nomodel.model.command.application.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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
     * 모델 삭제 시 캐시 무효화
     */
    @Caching(evict = {
            @CacheEvict(value = "modelDetail", key = "#modelId"),
            @CacheEvict(value = "modelSearch", allEntries = true)
    })
    public void evictOnModelDelete(Long modelId) {
        log.info("모델 삭제로 인한 캐시 무효화: modelId={}", modelId);
    }

    /**
     * 전체 검색 캐시 무효화
     * 대규모 변경이나 배치 작업 후 사용
     */
    public void evictAllSearchCaches() {
        evictCaches(Arrays.asList("modelSearch", "adminModels"));
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
     * 지정한 캐시 전체 무효화 (동기 실행)
     */
    public void evictCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("캐시 전체 삭제: {}", cacheName);
        }
    }

    /**
     * 여러 캐시 전체 무효화 (동기 실행)
     */
    public void evictCaches(List<String> cacheNames) {
        cacheNames.forEach(this::evictCache);
    }
}
