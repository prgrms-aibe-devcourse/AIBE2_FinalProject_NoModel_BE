package com.example.nomodel.model.application.scheduler;

import com.example.nomodel.model.application.service.LazyInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * LazyInvalidationService 배치를 구동하는 스케줄러.
 * 실행 타이밍만 제어하고 실제 로직은 서비스에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LazyInvalidationScheduler {

    private final LazyInvalidationService lazyInvalidationService;

    /**
     * 검색 캐시의 지연 무효화를 5분 주기로 실행한다.
     */
    @Scheduled(fixedDelay = 300000)
    public void triggerSearchCacheInvalidation() {
        log.trace("검색 캐시 지연 무효화 스케줄 실행");
        lazyInvalidationService.processDirtySearchCaches();
    }

    /**
     * 모델 캐시로부터 파생되는 검색 캐시 재마킹을 10분 주기로 실행한다.
     */
    @Scheduled(fixedDelay = 600000)
    public void triggerModelCacheInvalidation() {
        log.trace("모델 캐시 지연 무효화 스케줄 실행");
        lazyInvalidationService.processDirtyModelCaches();
    }
}
