package com.example.nomodel.model.query.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.file.domain.model.File;
import com.example.nomodel.file.domain.model.RelationType;
import com.example.nomodel.file.domain.repository.FileJpaRepository;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.command.application.dto.response.AIModelStaticDetail;
import com.example.nomodel.model.command.domain.model.document.AIModelDocument;
import com.example.nomodel.model.command.domain.model.AIModel;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.command.domain.repository.AIModelSearchRepository;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 모델 상세 조회 서비스
 *
 * 각 도메인 서비스를 조합하여 상세 조회 응답을 구성:
 * - AI 모델 기본 정보 (JPA)
 * - 검색 통계 정보 (Elasticsearch)
 * - 파일 정보 (File 도메인)
 * - 리뷰 정보 (Review 서비스)
 * - 소유자 정보 (Member 도메인)
 *
 * 조회수 증가는 ModelViewCountService에서 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelDetailService {

    private final AIModelJpaRepository aiModelRepository;
    private final AIModelSearchRepository searchRepository;
    private final FileJpaRepository fileRepository;
    private final ReviewService reviewService;
    private final MemberJpaRepository memberRepository;

    /**
     * AI 모델 상세 조회
     */
    @Transactional(readOnly = true)
    public AIModelStaticDetail getModelStaticDetail(Long modelId) {
        AIModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MODEL_NOT_FOUND));

        String ownerName = getOwnerName(model);
        List<AIModelDetailResponse.FileInfo> files = fileRepository
                .findImageFilesByRelation(RelationType.MODEL, modelId)
                .stream()
                .map(AIModelDetailResponse.FileInfo::from)
                .toList();

        return AIModelStaticDetail.builder()
                .modelId(model.getId())
                .modelName(model.getModelName())
                .description(model.getModelMetadata() != null ? model.getModelMetadata().getPrompt() : model.getModelName())
                .ownType(model.getOwnType().name())
                .ownerName(ownerName)
                .ownerId(model.getOwnerId())
                .price(model.getPrice())
                .files(files)
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    /**
     * Elasticsearch 문서 조회
     */
    private AIModelDocument findModelDocument(Long modelId) {
        Page<AIModelDocument> documents = searchRepository.findByModelId(modelId, PageRequest.of(0, 1));
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
     * 모델 리뷰 조회 (빈 리스트 또는 리뷰 목록 반환)
     */
    private List<ReviewResponse> getModelReviews(Long modelId) {
        return reviewService.getReviewsByModel(modelId);
    }
}