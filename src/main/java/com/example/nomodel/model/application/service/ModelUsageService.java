package com.example.nomodel.model.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryPageResponse;
import com.example.nomodel.model.application.dto.response.ModelUsageHistoryResponse;
import com.example.nomodel.model.domain.model.AdResult;
import com.example.nomodel.model.domain.repository.AdResultJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelUsageService {

    private final AdResultJpaRepository adResultRepository;
    private final FileJpaRepository fileRepository;

    /**
     * 회원의 모델 사용 내역 조회
     * @param userDetails 인증된 사용자 정보
     * @param modelId 특정 모델 ID (optional)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 모델 사용 내역 페이지
     */
    public ModelUsageHistoryPageResponse getModelUsageHistory(
            CustomUserDetails userDetails,
            Long modelId,
            int page,
            int size
    ) {
        if (userDetails == null || userDetails.getMemberId() == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }

        Long memberId = userDetails.getMemberId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<AdResult> adResultPage;
        
        if (modelId != null) {
            // 특정 모델의 사용 내역 조회
            log.info("회원 {}의 모델 {} 사용 내역 조회", memberId, modelId);
            adResultPage = adResultRepository.findByMemberIdAndModelIdOrderByCreatedAtDesc(
                memberId, modelId, pageable
            );
        } else {
            // 전체 모델 사용 내역 조회
            log.info("회원 {}의 전체 모델 사용 내역 조회", memberId);
            adResultPage = adResultRepository.findByMemberIdOrderByCreatedAtDesc(
                memberId, pageable
            );
        }
        
        // AdResult ID 목록 추출
        List<Long> adResultIds = adResultPage.getContent().stream()
            .map(AdResult::getId)
            .collect(Collectors.toList());
        
        // 각 AdResult에 대한 결과 이미지 파일 조회 (N+1 문제 방지)
        List<File> resultImageFiles = fileRepository.findByRelationTypeAndRelationIdIn(
            RelationType.AD_RESULT, adResultIds
        );
        
        // AdResult ID별로 이미지 URL 매핑
        Map<Long, String> imageUrlMap = resultImageFiles.stream()
            .filter(file -> file.getContentType().startsWith("image/"))
            .collect(Collectors.toMap(
                File::getRelationId,
                File::getFileUrl,
                (existing, replacement) -> existing // 중복 시 첫 번째 이미지 사용
            ));
        
        Page<ModelUsageHistoryResponse> responsePage = adResultPage.map(adResult -> 
            ModelUsageHistoryResponse.from(adResult, imageUrlMap.get(adResult.getId()))
        );
        
        return ModelUsageHistoryPageResponse.from(responsePage);
    }

    /**
     * 회원의 모델 사용 통계 조회
     * @param userDetails 인증된 사용자 정보
     * @return 모델 사용 통계
     */
    public long getModelUsageCount(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getMemberId() == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "인증된 사용자가 아닙니다.");
        }

        Long memberId = userDetails.getMemberId();
        return adResultRepository.countByMemberId(memberId);
    }
}