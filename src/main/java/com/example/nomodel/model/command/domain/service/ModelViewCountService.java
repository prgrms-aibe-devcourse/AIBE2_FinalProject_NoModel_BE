package com.example.nomodel.model.command.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.command.domain.model.ModelStatistics;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모델 조회수 관리 서비스
 * 캐시와 독립적으로 조회수 증가 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelViewCountService {

    private final ViewCountThrottleService throttleService;
    private final ModelStatisticsJpaRepository statisticsRepository;

    /**
     * 비동기 조회수 증가 처리 (중복 방지 체크 포함)
     * API 응답 속도에 영향을 주지 않도록 비동기 처리
     *
     * @param modelId 모델 ID
     * @param memberId 회원 ID (중복 방지용)
     */
    @Async("viewCountExecutor")
    @Transactional
    public void processViewCountAsync(Long modelId, Long memberId) {
        // 1. 중복 조회 체크
        if (!throttleService.canIncrementViewCount(modelId, memberId)) {
            log.debug("조회수 증가 스킵 (중복 방지): modelId={}, memberId={}", modelId, memberId);
            return;
        }

        // 2. 조회수 증가
        incrementViewCount(modelId);
    }

    /**
     * 조회수 직접 증가 (내부 사용)
     * 중복 체크 없이 단순 증가
     * processViewCountAsync의 트랜잭션 컨텍스트 내에서 실행됨
     */
    private void incrementViewCount(Long modelId) {
        ModelStatistics statistics = getModelStatistics(modelId);
        statistics.incrementViewCount();
        statisticsRepository.save(statistics);

        log.debug("모델 조회수 증가 완료: modelId={}, newCount={}",
                 modelId, statistics.getViewCount());
    }

    /**
     * ModelStatistics 조회
     * Model 생성 시 자동으로 ModelStatistics가 생성되므로 단순 조회만 수행
     */
    private ModelStatistics getModelStatistics(Long modelId) {
        return statisticsRepository.findByModelId(modelId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MODEL_NOT_FOUND));
    }
}