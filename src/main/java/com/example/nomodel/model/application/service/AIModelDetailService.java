package com.example.nomodel.model.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.domain.document.AIModelDocument;
import com.example.nomodel.model.domain.model.AIModel;
import com.example.nomodel.model.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.domain.repository.AIModelSearchRepository;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AI 모델 상세 조회 통합 서비스
 * 
 * 각 도메인 서비스를 조합하여 상세 조회 응답을 구성:
 * - AI 모델 기본 정보 (JPA)
 * - 검색 통계 정보 (Elasticsearch)  
 * - 파일 정보 (File 도메인)
 * - 리뷰 정보 (Review 서비스)
 * - 소유자 정보 (Member 도메인)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIModelDetailService {

    private final AIModelJpaRepository aiModelRepository;
    private final AIModelSearchRepository searchRepository;
    private final FileJpaRepository fileRepository;
    private final ReviewService reviewService;
    private final MemberJpaRepository memberRepository;

    /**
     * AI 모델 상세 조회
     */
    public AIModelDetailResponse getModelDetail(Long modelId) {
        // 1. AI 모델 기본 정보 조회
        AIModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MODEL_NOT_FOUND));
        
        // 2. Elasticsearch 문서에서 통계 정보 조회
        AIModelDocument document = findModelDocument(modelId);
        
        // 3. 소유자 정보 조회
        String ownerName = getOwnerName(model);
        
        // 4. 파일 정보 조회
        List<File> files = fileRepository.findImageFilesByRelation(RelationType.MODEL, modelId);
        
        // 5. 리뷰 정보 조회 (예외 발생하지 않도록 처리)
        List<ReviewResponse> reviews = getModelReviews(modelId);
        
        // 6. 응답 DTO 생성
        return AIModelDetailResponse.from(model, ownerName, document, files, reviews);
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void increaseViewCount(Long modelId) {
        // Elasticsearch 문서에서 사용량 증가
        AIModelDocument document = findModelDocument(modelId);
        if (document != null) {
            document.increaseUsage();
            searchRepository.save(document);
            log.debug("AI 모델 조회수 증가: modelId={}, viewCount={}", modelId, document.getUsageCount());
        }
    }

    /**
     * Elasticsearch 문서 조회
     */
    private AIModelDocument findModelDocument(Long modelId) {
        Page<AIModelDocument> documents = searchRepository.findByOwnerId(modelId, PageRequest.of(0, 1));
        return documents.getContent().isEmpty() ? null : documents.getContent().get(0);
    }

    /**
     * 소유자 이름 조회
     */
    private String getOwnerName(AIModel model) {
        if (model.getOwnerId() == null) {
            return model.getOwnType() != null ? model.getOwnType().name() : "ADMIN";
        }
        
        return memberRepository.findById(model.getOwnerId())
                .map(Member::getEmail)
                .map(Email::getValue)
                .orElse("Unknown");
    }

    /**
     * 모델 리뷰 조회 (예외 발생하지 않도록 처리)
     */
    private List<ReviewResponse> getModelReviews(Long modelId) {
        try {
            return reviewService.getReviewsByModel(modelId);
        } catch (ApplicationException e) {
            // 리뷰가 없는 경우 빈 리스트 반환
            if (e.getErrorCode() == ErrorCode.REVIEW_NOT_FOUND) {
                return List.of();
            }
            throw e;
        }
    }
}