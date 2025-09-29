package com.example.nomodel.model.query.service;

import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.command.application.dto.response.AIModelDynamicStats;
import com.example.nomodel.model.command.application.dto.response.AIModelStaticDetail;
import com.example.nomodel.model.command.domain.service.ModelStatisticsService;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.domain.model.ReviewStatus;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIModelDetailFacadeService {

    private final CachedModelDetailService cachedModelDetailService;
    private final ModelStatisticsService statisticsService;
    private final ReviewRepository reviewRepository;

    public AIModelDetailResponse getModelDetail(Long modelId, Long memberId) {
        // 정적 데이터 (캐시 적용)
        AIModelStaticDetail staticDetail = cachedModelDetailService.getModelStaticDetail(modelId);
        // 동적 데이터 (실시간 조회)
        AIModelDynamicStats dynamicStats = statisticsService.getDynamicStats(modelId, memberId);
        // 리뷰 목록 조회 (ACTIVE 상태만)
        List<ReviewResponse> reviews = reviewRepository
                .findByModelIdAndStatus(modelId, ReviewStatus.ACTIVE)
                .stream()
                .map(ReviewResponse::from) // Review → ReviewResponse 변환
                .toList();

        // 합쳐서 최종 응답 조립
        return AIModelDetailResponse.of(staticDetail, dynamicStats, reviews);
    }
}
