package com.example.nomodel.model.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.application.dto.response.AdResultAverageRatingResponseDto;
import com.example.nomodel.model.application.dto.response.AdResultCountResponseDto;
import com.example.nomodel.model.application.dto.response.AdResultResponseDto;
import com.example.nomodel.model.domain.model.AdResult;
import com.example.nomodel.model.domain.repository.AdResultJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdResultService {
    
    private final AdResultJpaRepository adResultRepository;
    
    /**
     * 회원의 AdResult 목록 조회
     */
    public Page<AdResultResponseDto> getMemberAdResults(Long memberId, Pageable pageable) {
        return adResultRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(AdResultResponseDto::from);
    }
    
    /**
     * 회원의 특정 AdResult 상세 조회
     */
    public AdResultResponseDto getMemberAdResult(Long memberId, Long adResultId) {
        AdResult adResult = adResultRepository.findByIdAndMemberId(adResultId, memberId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MODEL_NOT_FOUND));
        
        return AdResultResponseDto.from(adResult);
    }
    
    /**
     * 회원의 총 프로젝트 개수 조회
     */
    public AdResultCountResponseDto getMemberProjectCount(Long memberId) {
        long totalCount = adResultRepository.countProjectsByMemberId(memberId);
        return new AdResultCountResponseDto(totalCount);
    }
    
    /**
     * 회원의 평균 평점 조회
     */
    public AdResultAverageRatingResponseDto getMemberAverageRating(Long memberId) {
        Double averageRating = adResultRepository.findAverageRatingByMemberId(memberId);
        return AdResultAverageRatingResponseDto.from(averageRating);
    }
    
    /**
     * 회원의 AdResult 평점 업데이트
     */
    @Transactional
    public void updateAdResultRating(Long memberId, Long adResultId, Double rating) {
        AdResult adResult = adResultRepository.findByIdAndMemberId(adResultId, memberId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MODEL_NOT_FOUND));
        
        adResult.updateRating(rating);
        adResultRepository.save(adResult);
    }
}