package com.example.nomodel.model.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * 모델 검색 캐시 워밍 서비스
 * 자주 조회되는 데이터를 미리 캐시에 로드
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelCacheWarmingService {

    private final CachedModelSearchService cachedSearchService;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_WARM_PAGES = 3; // 처음 3페이지만 워밍

    /**
     * 애플리케이션 시작 시 캐시 워밍
     * 비동기로 실행하여 애플리케이션 시작을 지연시키지 않음
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCachesOnStartup() {
        log.info("애플리케이션 시작 - 캐시 워밍 시작");

        try {
            // 병렬로 여러 캐시 워밍 작업 실행
            CompletableFuture<Void> popularModels = warmUpPopularModels();
            CompletableFuture<Void> recentModels = warmUpRecentModels();
            CompletableFuture<Void> freeModels = warmUpFreeModels();
            CompletableFuture<Void> recommendedModels = warmUpRecommendedModels();
            CompletableFuture<Void> generalSearch = warmUpGeneralSearch();

            // 모든 작업 완료 대기
            CompletableFuture.allOf(
                    popularModels,
                    recentModels,
                    freeModels,
                    recommendedModels,
                    generalSearch
            ).join();

            log.info("캐시 워밍 완료");
        } catch (Exception e) {
            log.error("캐시 워밍 중 오류 발생", e);
        }
    }

    /**
     * 매일 새벽 4시에 캐시 재워밍
     * 캐시 정리 후 자주 사용되는 데이터 재로드
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Async
    public void scheduledCacheWarming() {
        log.info("스케줄된 캐시 워밍 시작");
        warmUpCachesOnStartup();
    }

    /**
     * 인기 모델 캐시 워밍
     */
    @Async
    public CompletableFuture<Void> warmUpPopularModels() {
        log.debug("인기 모델 캐시 워밍 시작");

        IntStream.range(0, MAX_WARM_PAGES)
                .forEach(page -> {
                    try {
                        cachedSearchService.getPopularModels(page, DEFAULT_PAGE_SIZE);
                        log.debug("인기 모델 캐시 워밍: page={}", page);
                    } catch (Exception e) {
                        log.warn("인기 모델 캐시 워밍 실패: page={}", page, e);
                    }
                });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 최신 모델 캐시 워밍
     */
    @Async
    public CompletableFuture<Void> warmUpRecentModels() {
        log.debug("최신 모델 캐시 워밍 시작");

        IntStream.range(0, 2) // 최신 모델은 2페이지만
                .forEach(page -> {
                    try {
                        cachedSearchService.getRecentModels(page, DEFAULT_PAGE_SIZE);
                        log.debug("최신 모델 캐시 워밍: page={}", page);
                    } catch (Exception e) {
                        log.warn("최신 모델 캐시 워밍 실패: page={}", page, e);
                    }
                });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 무료 모델 캐시 워밍
     */
    @Async
    public CompletableFuture<Void> warmUpFreeModels() {
        log.debug("무료 모델 캐시 워밍 시작");

        IntStream.range(0, MAX_WARM_PAGES)
                .forEach(page -> {
                    try {
                        cachedSearchService.getFreeModels(page, DEFAULT_PAGE_SIZE);
                        log.debug("무료 모델 캐시 워밍: page={}", page);
                    } catch (Exception e) {
                        log.warn("무료 모델 캐시 워밍 실패: page={}", page, e);
                    }
                });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 추천 모델 캐시 워밍
     */
    @Async
    public CompletableFuture<Void> warmUpRecommendedModels() {
        log.debug("추천 모델 캐시 워밍 시작");

        IntStream.range(0, 2) // 추천 모델은 2페이지만
                .forEach(page -> {
                    try {
                        cachedSearchService.getRecommendedModels(page, DEFAULT_PAGE_SIZE);
                        log.debug("추천 모델 캐시 워밍: page={}", page);
                    } catch (Exception e) {
                        log.warn("추천 모델 캐시 워밍 실패: page={}", page, e);
                    }
                });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 일반 검색 캐시 워밍 (자주 검색되는 키워드)
     */
    @Async
    public CompletableFuture<Void> warmUpGeneralSearch() {
        log.debug("일반 검색 캐시 워밍 시작");

        // 자주 검색되는 키워드 리스트 (실제로는 통계 기반으로 동적 생성 가능)
        List<String> popularKeywords = Arrays.asList(
                null,  // 전체 검색
                "AI",
                "이미지",
                "텍스트",
                "생성"
        );

        // 무료/유료 필터 조합
        List<Boolean> freeFilters = Arrays.asList(null, true, false);

        popularKeywords.forEach(keyword -> {
            freeFilters.forEach(isFree -> {
                IntStream.range(0, 2).forEach(page -> { // 첫 2페이지만
                    try {
                        cachedSearchService.search(keyword, isFree, page, DEFAULT_PAGE_SIZE);
                        log.debug("일반 검색 캐시 워밍: keyword={}, isFree={}, page={}",
                                keyword, isFree, page);
                    } catch (Exception e) {
                        log.warn("일반 검색 캐시 워밍 실패: keyword={}, isFree={}, page={}",
                                keyword, isFree, page, e);
                    }
                });
            });
        });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 자동완성 캐시 워밍
     * 자주 입력되는 prefix 기반
     */
    @Async
    public CompletableFuture<Void> warmUpAutoComplete() {
        log.debug("자동완성 캐시 워밍 시작");

        // 자주 사용되는 prefix (실제로는 통계 기반)
        List<String> commonPrefixes = Arrays.asList(
                "AI", "이미", "텍스", "생성", "모델",
                "im", "te", "ge", "mo", "ch"
        );

        commonPrefixes.forEach(prefix -> {
            try {
                cachedSearchService.getModelNameSuggestions(prefix);
                log.debug("자동완성 캐시 워밍: prefix={}", prefix);
            } catch (Exception e) {
                log.warn("자동완성 캐시 워밍 실패: prefix={}", prefix, e);
            }
        });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 관리자 모델 캐시 워밍
     */
    @Async
    public CompletableFuture<Void> warmUpAdminModels() {
        log.debug("관리자 모델 캐시 워밍 시작");

        IntStream.range(0, 2).forEach(page -> {
            try {
                cachedSearchService.getAdminModels(null, null, page, DEFAULT_PAGE_SIZE);
                log.debug("관리자 모델 캐시 워밍: page={}", page);
            } catch (Exception e) {
                log.warn("관리자 모델 캐시 워밍 실패: page={}", page, e);
            }
        });

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 수동 캐시 워밍 트리거
     * 관리자가 필요시 수동으로 캐시 워밍 실행
     */
    public void triggerManualCacheWarming() {
        log.info("수동 캐시 워밍 시작");
        warmUpCachesOnStartup();
    }

    /**
     * 선택적 캐시 워밍
     * 특정 캐시만 선택적으로 워밍
     */
    public void warmUpSpecificCache(String cacheType) {
        log.info("선택적 캐시 워밍 시작: type={}", cacheType);

        switch (cacheType.toLowerCase()) {
            case "popular":
                warmUpPopularModels();
                break;
            case "recent":
                warmUpRecentModels();
                break;
            case "free":
                warmUpFreeModels();
                break;
            case "recommended":
                warmUpRecommendedModels();
                break;
            case "search":
                warmUpGeneralSearch();
                break;
            case "autocomplete":
                warmUpAutoComplete();
                break;
            case "admin":
                warmUpAdminModels();
                break;
            default:
                log.warn("알 수 없는 캐시 타입: {}", cacheType);
        }
    }
}