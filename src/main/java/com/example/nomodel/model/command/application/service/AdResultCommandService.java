package com.example.nomodel.model.command.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.model.command.domain.model.AdResult;
import com.example.nomodel.model.command.domain.repository.AdResultJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdResultCommandService {

    private final AdResultJpaRepository adResultRepository;

    /**
     * 새로운 AdResult 생성 및 저장
     */
    @Transactional
    public AdResult createAdResult(Long modelId, Long memberId, String prompt, String adResultName) {
        AdResult adResult = AdResult.create(modelId, memberId, prompt, adResultName);
        return adResultRepository.save(adResult);
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

    /**
     * AdResult의 결과 이미지 URL 업데이트
     */
    @Transactional
    public void updateResultImageUrl(AdResult adResult, String resultFileUrl) {
        adResult.updateResultImageUrl(resultFileUrl);
        adResultRepository.save(adResult);
    }
}
