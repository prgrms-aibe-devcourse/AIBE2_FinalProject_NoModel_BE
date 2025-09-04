package com.example.nomodel.review.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.point.application.service.PointService;
import com.example.nomodel.point.domain.model.*;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.Rating;
import com.example.nomodel.review.domain.model.ReviewStatus;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PointService pointService;

    private final MemberPointBalanceRepository pointBalanceRepository;
    private final PointTransactionRepository transactionRepository;

    //리뷰 등록(중복 방지 후 저장, DTO 변환하여 반환)
    public ReviewResponse createReview(Long reviewerId, Long modelId, ReviewRequest request) {
        // 중복 리뷰 방지
        if (reviewRepository.existsByReviewerIdAndModelId(reviewerId, modelId)) {
            throw new ApplicationException(ErrorCode.DUPLICATE_REVIEW);
        }

        // 1. 리뷰 생성 및 저장
        ModelReview review = new ModelReview(
                reviewerId,
                modelId,
                new Rating(request.getRating()),
                request.getContent()
        );
        reviewRepository.save(review);

        // 2. 포인트 적립 로직 추가 PointService로 위임
        pointService.rewardForReview(reviewerId, review.getId());

        // 3. 리뷰 응답 반환
        return ReviewResponse.from(review);

    }

    //리뷰 조회(모델 ID로 리뷰들을 조회, 비어있으면 예외발생, 있으면 Dto 변환하여 반환)
    public List<ReviewResponse> getReviewsByModel(Long modelId) {
        List<ModelReview> reviews = reviewRepository.findByModelId(modelId);

        if (reviews.isEmpty()) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_FOUND);
        }

        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    //리뷰 수정 (본인만 가능)
    public ReviewResponse updateReview(Long reviewerId, Long reviewId, ReviewRequest request) {
        // 1. 리뷰 존재 여부 확인
        ModelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        // 2. 권한 확인 (본인만 가능)
        if (!review.getReviewerId().equals(reviewerId)) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // 3. 리뷰 내용 업데이트
        review.update(
                new Rating(request.getRating()),
                request.getContent()
        );

        // 4. 저장 후 DTO 변환
        return ReviewResponse.from(reviewRepository.save(review));
    }

    //리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewerId, Long reviewId) {
        ModelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getReviewerId().equals(reviewerId)) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        review.deactivate(ReviewStatus.DELETED);
        reviewRepository.save(review); // soft delete 반영
    }


}
