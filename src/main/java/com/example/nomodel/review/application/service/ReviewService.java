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

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

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
}
