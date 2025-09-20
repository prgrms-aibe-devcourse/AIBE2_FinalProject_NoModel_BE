package com.example.nomodel.model.command.application.cache.service;

import com.example.nomodel.model.command.application.dto.response.cache.BatchStatisticsResponse;
import com.example.nomodel.model.command.application.dto.response.cache.LazyInvalidationStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // Redis 키 상수
    private static final String DIRTY_SEARCH_PREFIX = "cache:dirty:search:"; // 검색 캐시 마킹
    private static final String BATCH_STATS_KEY = "cache:batch_stats";

    /**
     * 검색 캐시를 지연 무효화 대상으로 마킹
     */
    public void markSearchCacheDirty(String cacheName) {
        String key = DIRTY_SEARCH_PREFIX + cacheName;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        redisTemplate.opsForHash().put(key, "ALL", timestamp);
        redisTemplate.expire(key, 1, TimeUnit.HOURS); // 1시간 후 자동 삭제

        log.debug("검색 캐시 dirty 마킹: cache={}, key=ALL", cacheName);
    }

    /**
     * 검색 캐시 배치 무효화 (5분마다)
     * 피크 시간을 피해 DB 부하 분산
     */
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
                    redisTemplate.delete(dirtyKey);
                    continue;
                }

                cacheEvictionService.evictCache(cacheName);

                redisTemplate.delete(dirtyKey);
                processedCount++;

                log.debug("검색 캐시 배치 무효화 처리: cache={}, dirty_items={}", cacheName, dirtyItems.size());
            }

            recordBatchStats("search_cache", processedCount);

            log.info("검색 캐시 배치 무효화 완료: processed={}", processedCount);

        } catch (Exception e) {
            log.error("검색 캐시 배치 무효화 실패", e);
        }
    }





    /**
     * 배치 처리 통계 기록 (단기 모니터링을 위해 Redis 활용)
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

        log.warn("긴급 dirty 캐시 즉시 처리 완료");
    }

    /**
     * 지연 무효화 서비스 상태 정보 반환
     */
    public LazyInvalidationStatusResponse getStatus() {
        Set<String> dirtySearchKeys = redisTemplate.keys(DIRTY_SEARCH_PREFIX + "*");
        long searchCount = dirtySearchKeys.size();
        long modelCount = 0L;

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

            return new BatchStatisticsResponse(
                    searchCacheCount,
                    searchCacheLastRun,
                    0L,
                    null
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
