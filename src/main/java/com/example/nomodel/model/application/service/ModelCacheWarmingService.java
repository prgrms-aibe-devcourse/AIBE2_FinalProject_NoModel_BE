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
    private static final int MAX_WARM_PAGES = 2; // 첫 2페이지만 워밍

    /**
     * 애플리케이션 시작 시 캐시 워밍
     * 비동기로 실행하여 애플리케이션 시작을 지연시키지 않음
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCachesOnStartup() {
        log.info("애플리케이션 시작 - 캐시 워밍 시작");

        try {
            // 향후 추가 워밍 작업을 대비해 비동기 Future 형태 유지
            CompletableFuture<Void> generalSearch = warmUpGeneralSearch();

            // 모든 작업 완료 대기
            CompletableFuture.allOf(
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
                IntStream.range(0, MAX_WARM_PAGES).forEach(page -> {
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

}
