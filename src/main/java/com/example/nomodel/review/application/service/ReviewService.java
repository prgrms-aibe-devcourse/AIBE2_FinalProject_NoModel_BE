package com.example.nomodel.review.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.point.application.service.PointService;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.Rating;
import com.example.nomodel.review.domain.model.ReviewStatus;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    @Transactional
    public ReviewResponse createReview(Long reviewerId, Long modelId, ReviewRequest request) {
        // ACTIVE 상태인 리뷰만 중복 검사
        if (reviewRepository.existsByReviewerIdAndModelIdAndStatus(reviewerId, modelId, ReviewStatus.ACTIVE)) {
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

    //리뷰 조회(모델 ID로 리뷰들을 조회, 빈 리스트 또는 Dto 리스트 반환)
    public List<ReviewResponse> getReviewsByModel(Long modelId) {
        // ACTIVE 리뷰만 조회
        List<ModelReview> reviews = reviewRepository.findByModelIdAndStatus(modelId, ReviewStatus.ACTIVE);

        // 리뷰가 없어도 정상적인 상황이므로 빈 리스트 반환
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

        //3. 삭제된 리뷰 수정 불가(상태 체크)
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // 4. 리뷰 내용 업데이트
        review.update(
                new Rating(request.getRating()),
                request.getContent()
        );

        // 5. 저장 후 DTO 변환
        return ReviewResponse.from(reviewRepository.save(review));
    }

    //리뷰 삭제
    @Transactional
    public ReviewResponse deleteReview(Long reviewerId, Long reviewId) {
        ModelReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getReviewerId().equals(reviewerId)) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        review.deactivate(ReviewStatus.DELETED);
        ModelReview deleted = reviewRepository.save(review);

        return ReviewResponse.from(deleted);
    }

    //내가 작성한 모든 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyAllReviews(Long reviewerId) {
        // ACTIVE 상태인 내가 작성한 모든 리뷰 조회 (최신순 정렬)
        List<ModelReview> myReviews = reviewRepository.findByReviewerIdAndStatusOrderByCreatedAtDesc(
                reviewerId,
                ReviewStatus.ACTIVE
        );

        return myReviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 내가 작성한 특정 리뷰 조회 (권한 체크 포함)
     */
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long reviewerId, Long reviewId) {
        ModelReview review = reviewRepository.findByIdAndReviewerId(reviewId, reviewerId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        // 삭제된 리뷰는 조회 불가
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_FOUND);
        }

        return ReviewResponse.from(review);
    }

    /**
     * 내가 작성한 리뷰 수정 (권한 체크 포함)
     */
    @Transactional
    public ReviewResponse updateMyReview(Long reviewerId, Long reviewId, ReviewRequest request) {
        ModelReview review = reviewRepository.findByIdAndReviewerId(reviewId, reviewerId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        // 삭제된 리뷰 수정 불가
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ApplicationException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // 리뷰 내용 업데이트
        review.update(
                new Rating(request.getRating()),
                request.getContent()
        );

        return ReviewResponse.from(reviewRepository.save(review));
    }


    //내가 작성한 리뷰 삭제(권한 포함)
    @Transactional
    public void deleteMyReview(Long reviewerId, Long reviewId) {
        ModelReview review = reviewRepository.findByIdAndReviewerId(reviewId, reviewerId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        review.deactivate(ReviewStatus.DELETED);
        reviewRepository.save(review);
    }


   // 특정 모델에서 내가 작성한 리뷰 조회

    @Transactional(readOnly = true)
    public ReviewResponse getMyReviewByModel(Long reviewerId, Long modelId) {
        ModelReview review = reviewRepository.findByReviewerIdAndModelIdAndStatus(
                reviewerId, modelId, ReviewStatus.ACTIVE
        ).orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        return ReviewResponse.from(review);
    }


     //특정 모델에서 내 리뷰 수정
    @Transactional
    public ReviewResponse updateMyReviewByModel(Long reviewerId, Long modelId, ReviewRequest request) {
        ModelReview review = reviewRepository.findByReviewerIdAndModelIdAndStatus(
                reviewerId, modelId, ReviewStatus.ACTIVE
        ).orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        // 리뷰 내용 업데이트
        review.update(
                new Rating(request.getRating()),
                request.getContent()
        );

        return ReviewResponse.from(reviewRepository.save(review));
    }


     //특정 모델에서 내 리뷰 삭제
    @Transactional
    public void deleteMyReviewByModel(Long reviewerId, Long modelId) {
        ModelReview review = reviewRepository.findByReviewerIdAndModelIdAndStatus(
                reviewerId, modelId, ReviewStatus.ACTIVE
        ).orElseThrow(() -> new ApplicationException(ErrorCode.REVIEW_NOT_FOUND));

        review.deactivate(ReviewStatus.DELETED);
        reviewRepository.save(review);
    }
}

