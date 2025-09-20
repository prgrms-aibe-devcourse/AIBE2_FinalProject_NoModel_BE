package com.example.nomodel.model.application.service;

import com.example.nomodel.model.application.dto.response.cache.SmartCacheStatusResponse;
import com.example.nomodel.model.domain.event.ModelCreatedEvent;
import com.example.nomodel.model.domain.event.ModelDeletedEvent;
import com.example.nomodel.model.domain.event.ModelUpdateEvent;
import com.example.nomodel.review.application.event.ReviewEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 스마트 캐시 무효화 서비스
 * 선택적 업데이트와 지연 무효화를 통한 효율적인 캐시 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartCacheEvictionService {

    private final ModelCacheEvictionService cacheEvictionService;
    private final ModelCacheService modelCacheService;
    private final RedisTemplate<String, String> redisTemplate;

    // 지연 무효화용 Redis 키
    private static final String DIRTY_MODELS_KEY = "cache:dirty_models";
    private static final String DIRTY_SEARCH_KEY = "cache:dirty_search";

    /**
     * 모델 생성 시 캐시 처리
     * 즉시 검색 캐시에 추가 (성능 중요)
     */
    @EventListener
    @Async
    public void onModelCreated(ModelCreatedEvent event) {
        log.info("모델 생성 이벤트 처리: modelId={}, isPublic={}",
                event.getModelId(), event.isPublic());

        if (event.isPublic()) {
            markSearchCacheDirty("modelSearch");

            if ("ADMIN".equalsIgnoreCase(event.getOwnType())) {
                markSearchCacheDirty("adminModels");
            }
        }
    }

    /**
     * 모델 업데이트 시 선택적 캐시 처리
     */
    @EventListener
    @Async
    public void onModelUpdated(ModelUpdateEvent event) {
        log.info("모델 업데이트 이벤트: modelId={}, type={}",
                event.getModelId(), event.getUpdateType());

        switch (event.getUpdateType()) {
            case "PRICE":
                handlePriceUpdate(event);
                break;
            case "VISIBILITY":
                handleVisibilityUpdate(event);
                break;
            case "BASIC_INFO":
                handleBasicInfoUpdate(event);
                break;
            case "FILES":
                handleFilesUpdate(event);
                break;
            default:
                // 기본: 모델 상세만 캐시 갱신
                modelCacheService.updateModelDetailCache(event.getModelId());
        }
    }

    /**
     * 모델 삭제 시 즉시 무효화 (필수)
     */
    @EventListener
    @Async
    public void onModelDeleted(ModelDeletedEvent event) {
        log.info("모델 삭제 이벤트: modelId={}", event.getModelId());

        // 삭제는 즉시 처리 (잘못된 데이터 노출 방지)
        cacheEvictionService.evictOnModelDelete(event.getModelId());
    }

    /**
     * 리뷰 변경 시 선택적 캐시 갱신
     */
    @EventListener
    @Async
    public void onReviewChanged(ReviewEvent event) {
        log.info("리뷰 변경 이벤트: modelId={}, action={}",
                event.getModelId(), event.getAction());

        // 모델 상세는 즉시 갱신 (리뷰 정보 포함)
        modelCacheService.updateModelDetailCache(event.getModelId());

        // 평점 변화는 검색 결과 순서에 영향을 주므로 검색 캐시 지연 무효화
        markSearchCacheDirty("modelSearch");
        markSearchCacheDirty("adminModels");
    }

    /**
     * 가격 변경 처리 - 중요도 높음
     */
    private void handlePriceUpdate(ModelUpdateEvent event) {
        BigDecimal oldPrice = (BigDecimal) event.getOldValue();
        BigDecimal newPrice = (BigDecimal) event.getNewValue();

        boolean wasFreeBefore = isPrice(oldPrice);
        boolean isFreeNow = isPrice(newPrice);

        // 모델 상세 즉시 갱신
        modelCacheService.updateModelDetailCache(event.getModelId());

        // 무료 상태 변경 시 즉시 캐시 무효화
        if (wasFreeBefore != isFreeNow) {
            cacheEvictionService.evictCache("modelSearch");
            cacheEvictionService.evictCache("adminModels");
        }

        // 기타 검색 캐시는 지연 처리
        markSearchCacheDirty("modelSearch");
        markSearchCacheDirty("adminModels");
    }

    /**
     * 공개 상태 변경 - 즉시 처리 필요
     */
    private void handleVisibilityUpdate(ModelUpdateEvent event) {
        Boolean newVisibility = (Boolean) event.getNewValue();

        // 모델 상세 즉시 갱신
        modelCacheService.updateModelDetailCache(event.getModelId());

        if (Boolean.TRUE.equals(newVisibility)) {
            // 공개로 변경 - 검색 결과에 나타나야 함
            markSearchCacheDirty("modelSearch");
            markSearchCacheDirty("adminModels");
        } else {
            // 비공개로 변경 - 즉시 제거
            cacheEvictionService.evictAllSearchCaches();
        }
    }

    /**
     * 기본 정보 변경 - 점진적 갱신
     */
    private void handleBasicInfoUpdate(ModelUpdateEvent event) {
        // 모델 상세 즉시 갱신
        modelCacheService.updateModelDetailCache(event.getModelId());

        // 검색 관련은 지연 처리
        markSearchCacheDirty("modelSearch");
        markSearchCacheDirty("adminModels");
    }

    /**
     * 파일 변경 - 모델 상세만 영향
     */
    private void handleFilesUpdate(ModelUpdateEvent event) {
        // 파일 정보는 모델 상세에만 포함
        modelCacheService.updateModelDetailCache(event.getModelId());
    }

    /**
     * 검색 캐시를 dirty로 마킹 (지연 무효화)
     */
    private void markSearchCacheDirty(String cacheName) {
        redisTemplate.opsForSet().add(DIRTY_SEARCH_KEY, cacheName);
        log.debug("검색 캐시 dirty 마킹: {}", cacheName);
    }

    /**
     * 배치 처리: Dirty 캐시들을 주기적으로 정리
     * 5분마다 실행하여 DB 부하 분산
     */
    @Scheduled(fixedDelay = 300000) // 5분
    public void processDirtySearchCaches() {
        Set<String> dirtyCaches = redisTemplate.opsForSet().members(DIRTY_SEARCH_KEY);

        if (dirtyCaches != null && !dirtyCaches.isEmpty()) {
            log.info("Dirty 검색 캐시 배치 정리 시작: count={}", dirtyCaches.size());

            // 배치로 무효화
            cacheEvictionService.evictCachesAsync(dirtyCaches.stream().toList());

            // dirty 마킹 정리
            redisTemplate.delete(DIRTY_SEARCH_KEY);

            log.info("Dirty 검색 캐시 배치 정리 완료");
        }
    }

    /**
     * 특정 모델 긴급 캐시 무효화
     * 잘못된 데이터가 캐싱된 경우 즉시 제거
     */
    public void emergencyEviction(Long modelId, String reason) {
        log.error("긴급 모델 캐시 무효화: modelId={}, reason={}", modelId, reason);

        // 1. 해당 모델 상세 캐시 즉시 삭제
        cacheEvictionService.evictSpecificCacheKey("modelDetail", modelId);

        // 2. 모든 검색 캐시 즉시 무효화 (해당 모델이 포함될 수 있음)
        cacheEvictionService.evictAllSearchCaches();

        // 3. 관련 dirty 마킹 즉시 정리
        redisTemplate.delete(DIRTY_SEARCH_KEY);
        redisTemplate.delete(DIRTY_MODELS_KEY + modelId);

        // 5. 긴급 상황 로그 및 알림
        logEmergencyAction(modelId, reason);
    }

    /**
     * 긴급 상황 로그 및 알림
     */
    private void logEmergencyAction(Long modelId, String reason) {
        // 구조화된 로그 생성
        log.error("=== EMERGENCY CACHE EVICTION ===");
        log.error("Model ID: {}", modelId);
        log.error("Reason: {}", reason);
        log.error("Timestamp: {}", java.time.LocalDateTime.now());
        log.error("Affected Caches: modelDetail, all search caches");
        log.error("===================================");

        // TODO: 실제 운영 시 슬랙/이메일 알림 연동
        // alertService.sendCriticalAlert("긴급 캐시 무효화", modelId, reason);
    }

    /**
     * 가격이 무료인지 체크
     */
    private boolean isPrice(BigDecimal price) {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 캐시 상태 정보 반환
     */
    public SmartCacheStatusResponse getCacheStatus() {
        Set<String> dirtyCaches = redisTemplate.opsForSet().members(DIRTY_SEARCH_KEY);
        long dirtyCount = dirtyCaches != null ? dirtyCaches.size() : 0;
        List<String> dirtyCacheList = dirtyCaches != null ?
                dirtyCaches.stream().sorted().toList() : List.of();

        return new SmartCacheStatusResponse(
                "SmartCacheEvictionService",
                dirtyCount,
                dirtyCacheList,
                LocalDateTime.now()
        );
    }
}
