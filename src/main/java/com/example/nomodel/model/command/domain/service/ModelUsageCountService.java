package com.example.nomodel.model.command.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.command.domain.model.ModelStatistics;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelUsageCountService {

    private final ModelStatisticsJpaRepository statisticsRepository;

    /**
     * 모델 사용 횟수 증가
     * @param modelId 모델 ID
     */
    @Transactional
    public void incrementUsageCount(Long modelId) {
        ModelStatistics statistics = getModelStatistics(modelId);
        statistics.incrementUsageCount();
        statisticsRepository.save(statistics);

        log.debug("모델 사용 횟수 증가 완료: modelId={}, newCount={}",
                 modelId, statistics.getUsageCount());
    }

    private ModelStatistics getModelStatistics(Long modelId) {
        return statisticsRepository.findByModelId(modelId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MODEL_NOT_FOUND));
    }
}
