package com.example.nomodel.model.command.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.command.application.dto.response.AIModelDynamicStats;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.model.ModelStatistics;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.review.domain.model.ReviewStatus;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelStatisticsService {

    private final ModelStatisticsJpaRepository modelStatisticsRepository;
    private final ReviewRepository modelReviewRepository;

    @Transactional(readOnly = true)
    public AIModelDynamicStats getDynamicStats(Long modelId, Long memberId) {

        ModelStatistics stats = modelStatisticsRepository.findByModelId(modelId).orElse(null);

        // 2. 리뷰 데이터 조회 (status = ACTIVE)
        Double avgRating = modelReviewRepository.calculateAverageRatingByModelId(
                modelId, ReviewStatus.ACTIVE
        );
        Long reviewCount = modelReviewRepository.countByModelIdAndStatus(
                modelId, ReviewStatus.ACTIVE
        );

        // 3. 조립
        return AIModelDynamicStats.builder()
                .avgRating(avgRating != null ? avgRating : 0.0)
                .reviewCount(reviewCount)
                .usageCount(stats != null ? stats.getUsageCount() : 0L)
                .viewCount(stats != null ? stats.getViewCount() : 0L)
                .build();
    }

    @Transactional
    public void createInitialStatistics(AIModel model) {
        ModelStatistics statistics = ModelStatistics.createInitialStatistics(model);
        modelStatisticsRepository.save(statistics);
    }
}
