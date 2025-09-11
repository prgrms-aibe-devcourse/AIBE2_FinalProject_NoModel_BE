package com.example.nomodel.model.application.service;

import com.example.nomodel.model.application.dto.ModelUsageProjection;
import com.example.nomodel.model.application.dto.response.ModelUsageCountResponse;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryResponse;
import com.example.nomodel.model.domain.repository.ModelUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelUsageService {

    private final ModelUsageRepository modelUsageRepository;

    /**
     * 회원의 모델 사용 내역 조회
     * @param memberId 회원 ID
     * @param modelId 특정 모델 ID (optional)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 모델 사용 내역 페이지
     */
    public ModelUsageHistoryPageResponse getModelUsageHistory(
            Long memberId,
            Long modelId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        // 조인 쿼리를 사용하여 한 번에 모든 데이터 조회
        Page<ModelUsageProjection> projectionPage = (modelId != null)
            ? modelUsageRepository.findModelUsageByMemberIdAndModelId(memberId, modelId, pageable)
            : modelUsageRepository.findModelUsageByMemberId(memberId, pageable);
        
        // Projection을 Response DTO로 변환
        Page<ModelUsageHistoryResponse> responsePage = projectionPage.map(projection -> 
            ModelUsageHistoryResponse.builder()
                .adResultId(projection.getAdResultId())
                .modelId(projection.getModelId())
                .modelName(projection.getModelName())
                .modelImageUrl(projection.getModelImageUrl())
                .prompt(projection.getPrompt())
                .createdAt(projection.getCreatedAt())
                .build()
        );
        
        return ModelUsageHistoryPageResponse.from(responsePage);
    }

    /**
     * 회원의 모델 사용 통계 조회
     * @param memberId 회원 ID
     * @return 모델 사용 통계
     */
    public ModelUsageCountResponse getModelUsageCount(Long memberId) {
        long count = modelUsageRepository.countModelUsageByMemberId(memberId);
        return ModelUsageCountResponse.from(count);
    }
}