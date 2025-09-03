package com.example.nomodel.review.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.Rating;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    //리뷰 등록(중복 방지 후 저장, DTO 변환하여 반환)
    public ReviewResponse createReview(Long reviewerId, Long modelId, ReviewRequest request) {
        // 중복 리뷰 방지
        if (reviewRepository.existsByReviewerIdAndModelId(reviewerId, modelId)) {
            throw new ApplicationException(ErrorCode.DUPLICATE_REVIEW);
        }

        ModelReview review = new ModelReview(
                reviewerId,
                modelId,
                new Rating(request.getRating()),
                request.getContent()
        );

        return ReviewResponse.from(reviewRepository.save(review));
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
}
