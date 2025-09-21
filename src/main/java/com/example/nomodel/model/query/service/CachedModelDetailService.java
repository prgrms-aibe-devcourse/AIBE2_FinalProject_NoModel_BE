package com.example.nomodel.model.query.service;

import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.command.domain.service.ModelViewCountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐시가 적용된 AI 모델 상세 조회 서비스
 * 조회수 증가와 캐시를 분리하여 처리
 */
@Slf4j
@Service
public class CachedModelDetailService {

    private final AIModelDetailService modelDetailService;
    private final ModelViewCountService viewCountService;
    private final CachedModelDetailService self; // AOP 적용된 프록시

    public CachedModelDetailService(AIModelDetailService modelDetailService,
                                   ModelViewCountService viewCountService,
                                   @Lazy CachedModelDetailService self) {
        this.modelDetailService = modelDetailService;
        this.viewCountService = viewCountService;
        this.self = self;
    }

    /**
     * AI 모델 상세 조회 (조회수 증가 포함)
     *
     * 조회수 증가와 캐시 조회를 분리하여 처리:
     * - 조회수는 캐시 상태와 무관하게 항상 증가
     * - 상세 정보는 캐시에서 조회
     *
     * @param modelId 모델 ID
     * @param memberId 회원 ID (중복 방지용)
     * @return 모델 상세 정보
     */
    public AIModelDetailResponse getModelDetailWithView(Long modelId, Long memberId) {
        // 1. 조회수 증가 처리 (캐시와 독립적으로 항상 실행)
        viewCountService.processViewCountAsync(modelId, memberId);

        // 2. 캐시된 모델 상세 정보 조회 (프록시를 통해 호출하여 AOP 적용)
        return self.getModelDetail(modelId);
    }

    /**
     * AI 모델 상세 조회 (캐시 전용)
     * 캐시 히트/미스에 따라 DB 조회 여부 결정
     *
     * @param modelId 모델 ID
     * @return 모델 상세 정보
     */
    @Cacheable(
            value = "modelDetail",
            key = "#modelId",
            unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public AIModelDetailResponse getModelDetail(Long modelId) {
        log.debug("캐시 미스 - 모델 상세 조회 실행: modelId={}", modelId);
        return modelDetailService.getModelDetail(modelId);
    }
}