package com.example.nomodel.model.application.service;

import com.example.nomodel.model.application.dto.response.cache.BatchStatisticsResponse;
import com.example.nomodel.model.application.dto.response.cache.LazyInvalidationStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 지연 무효화 서비스
 * 검색 캐시에 대한 배치 무효화로 DB 부하 분산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LazyInvalidationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ModelCacheEvictionService cacheEvictionService;
    private final CachedModelSearchService cachedSearchService;

    // Redis 키 상수
    private static final String DIRTY_SEARCH_PREFIX = "cache:dirty:search:";
    private static final String DIRTY_MODELS_PREFIX = "cache:dirty:models:";
    private static final String BATCH_STATS_KEY = "cache:batch_stats";

    /**
     * 검색 캐시를 지연 무효화 대상으로 마킹
     */
    public void markSearchCacheDirty(String cacheName) {
        markSearchCacheDirty(cacheName, null);
    }

    /**
     * 특정 키워드/조건의 검색 캐시를 dirty로 마킹
     */
    public void markSearchCacheDirty(String cacheName, String searchKey) {
        String key = DIRTY_SEARCH_PREFIX + cacheName;
        String value = searchKey != null ? searchKey : "ALL";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        redisTemplate.opsForHash().put(key, value, timestamp);
        redisTemplate.expire(key, 1, TimeUnit.HOURS); // 1시간 후 자동 삭제

        log.debug("검색 캐시 dirty 마킹: cache={}, key={}", cacheName, value);
    }

    /**
     * 검색 캐시 배치 무효화 (5분마다)
     * 피크 시간을 피해 DB 부하 분산
     */
    @Scheduled(fixedDelay = 300000) // 5분
    public void processDirtySearchCaches() {
        try {
            Set<String> dirtyKeys = redisTemplate.keys(DIRTY_SEARCH_PREFIX + "*");

            if (dirtyKeys.isEmpty()) {
                return;
            }

            log.info("검색 캐시 배치 무효화 시작: dirty_count={}", dirtyKeys.size());

            int processedCount = 0;
            for (String dirtyKey : dirtyKeys) {
                String cacheName = dirtyKey.substring(DIRTY_SEARCH_PREFIX.length());

                // 해당 캐시의 모든 dirty 항목 조회
                Set<Object> dirtyItems = redisTemplate.opsForHash().keys(dirtyKey);

                if (dirtyItems.isEmpty()) {
                    continue;
                }

                // 캐시 타입별 처리
                processSearchCacheByType(cacheName, dirtyItems);

                // dirty 마킹 제거
                redisTemplate.delete(dirtyKey);
                processedCount++;
            }

            // 배치 통계 기록
            recordBatchStats("search_cache", processedCount);

            log.info("검색 캐시 배치 무효화 완료: processed={}", processedCount);

        } catch (Exception e) {
            log.error("검색 캐시 배치 무효화 실패", e);
        }
    }

    /**
     * 모델 캐시 배치 처리 (10분마다)
     */
    @Scheduled(fixedDelay = 600000) // 10분
    public void processDirtyModelCaches() {
        try {
            Set<String> dirtyKeys = redisTemplate.keys(DIRTY_MODELS_PREFIX + "*");

            if (dirtyKeys.isEmpty()) {
                return;
            }

            log.info("모델 캐시 배치 처리 시작: dirty_count={}", dirtyKeys.size());

            List<Long> modelIds = dirtyKeys.stream()
                    .map(key -> key.substring(DIRTY_MODELS_PREFIX.length()))
                    .map(Long::valueOf)
                    .toList();

            // 배치로 처리
            processDirtyModels(modelIds);

            // dirty 마킹 제거
            redisTemplate.delete(dirtyKeys);

            recordBatchStats("model_cache", modelIds.size());

            log.info("모델 캐시 배치 처리 완료: processed={}", modelIds.size());

        } catch (Exception e) {
            log.error("모델 캐시 배치 처리 실패", e);
        }
    }

    /**
     * 검색 캐시 타입별 처리
     */
    private void processSearchCacheByType(String cacheName, Set<Object> dirtyItems) {
        switch (cacheName) {
            case "modelSearch":
                // 전체 검색 캐시 무효화
                cacheEvictionService.evictSpecificCacheKey("modelSearch", null);
                break;

            case "popularModels":
                // 인기 모델 캐시 재구성
                refreshPopularModelsCache();
                break;

            case "freeModels":
                // 무료 모델 캐시 재구성
                refreshFreeModelsCache();
                break;

            case "recentModels":
                // 최신 모델 캐시 재구성
                refreshRecentModelsCache();
                break;

            case "autoComplete":
                // 자동완성 캐시 무효화
                cacheEvictionService.evictSpecificCacheKey("autoComplete", null);
                break;

            default:
                // 기본: 무효화
                cacheEvictionService.evictSpecificCacheKey(cacheName, null);
        }

        log.debug("검색 캐시 타입별 처리 완료: type={}, items={}", cacheName, dirtyItems.size());
    }

    /**
     * Dirty 모델들 배치 처리
     */
    private void processDirtyModels(List<Long> modelIds) {
        // 모델별로 개별 처리보다는 영향받는 검색 캐시를 일괄 갱신
        if (!modelIds.isEmpty()) {
            markSearchCacheDirty("modelSearch");
            markSearchCacheDirty("popularModels");

            log.debug("Dirty 모델들로 인한 검색 캐시 재마킹: models={}", modelIds.size());
        }
    }

    /**
     * 인기 모델 캐시 재구성
     */
    private void refreshPopularModelsCache() {
        try {
            // 첫 페이지만 즉시 재구성
            cachedSearchService.refreshPopularModelsCache(0, 20);
            log.debug("인기 모델 캐시 재구성 완료");
        } catch (Exception e) {
            log.warn("인기 모델 캐시 재구성 실패", e);
            cacheEvictionService.evictSpecificCacheKey("popularModels", null);
        }
    }

    /**
     * 무료 모델 캐시 재구성
     */
    private void refreshFreeModelsCache() {
        try {
            for (int page = 0; page < 3; page++) { // 첫 3페이지
                cachedSearchService.getFreeModels(page, 20);
            }
            log.debug("무료 모델 캐시 재구성 완료");
        } catch (Exception e) {
            log.warn("무료 모델 캐시 재구성 실패", e);
            cacheEvictionService.evictSpecificCacheKey("freeModels", null);
        }
    }

    /**
     * 최신 모델 캐시 재구성
     */
    private void refreshRecentModelsCache() {
        try {
            cachedSearchService.refreshRecentModelsCache(0, 20);
            log.debug("최신 모델 캐시 재구성 완료");
        } catch (Exception e) {
            log.warn("최신 모델 캐시 재구성 실패", e);
            cacheEvictionService.evictSpecificCacheKey("recentModels", null);
        }
    }

    /**
     * 배치 처리 통계 기록
     */
    private void recordBatchStats(String type, int count) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = BATCH_STATS_KEY + ":" + today;

            redisTemplate.opsForHash().increment(key, type + "_count", count);
            redisTemplate.opsForHash().put(key, type + "_last_run",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            redisTemplate.expire(key, 7, TimeUnit.DAYS); // 7일 보관

        } catch (Exception e) {
            log.warn("배치 통계 기록 실패: type={}", type, e);
        }
    }

    /**
     * 긴급 처리: 모든 dirty 마킹 즉시 처리
     */
    public void processAllDirtyImmediately() {
        log.warn("긴급 dirty 캐시 즉시 처리 시작");

        processDirtySearchCaches();
        processDirtyModelCaches();

        log.warn("긴급 dirty 캐시 즉시 처리 완료");
    }

    /**
     * 지연 무효화 서비스 상태 정보 반환
     */
    public LazyInvalidationStatusResponse getStatus() {
        Set<String> dirtySearchKeys = redisTemplate.keys(DIRTY_SEARCH_PREFIX + "*");
        Set<String> dirtyModelKeys = redisTemplate.keys(DIRTY_MODELS_PREFIX + "*");

        long searchCount = dirtySearchKeys.size();
        long modelCount = dirtyModelKeys.size();

        // 배치 통계 조회
        BatchStatisticsResponse batchStats = getBatchStatistics();

        return new LazyInvalidationStatusResponse(
                "LazyInvalidationService",
                searchCount,
                modelCount,
                batchStats,
                LocalDateTime.now()
        );
    }

    /**
     * 배치 통계 정보 반환
     */
    private BatchStatisticsResponse getBatchStatistics() {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = BATCH_STATS_KEY + ":" + today;

            Map<Object, Object> stats = redisTemplate.opsForHash().entries(key);

            Long searchCacheCount = parseStatValue(stats.get("search_cache_count"));
            LocalDateTime searchCacheLastRun = parseDateTime((String) stats.get("search_cache_last_run"));
            Long modelCacheCount = parseStatValue(stats.get("model_cache_count"));
            LocalDateTime modelCacheLastRun = parseDateTime((String) stats.get("model_cache_last_run"));

            return new BatchStatisticsResponse(
                    searchCacheCount,
                    searchCacheLastRun,
                    modelCacheCount,
                    modelCacheLastRun
            );
        } catch (Exception e) {
            log.warn("배치 통계 조회 실패", e);
            return new BatchStatisticsResponse(0L, null, 0L, null);
        }
    }

    private Long parseStatValue(Object value) {
        if (value == null) return 0L;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}