package com.example.nomodel.review.application.service;

import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.Rating;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewResponse createReview(Long reviewerId, Long modelId, ReviewRequest request) {
        ModelReview review = new ModelReview(
                reviewerId,
                modelId,
                new Rating(request.getRating()),
                request.getContent()
        );

        ModelReview saved = reviewRepository.save(review);

        return new ReviewResponse(
                saved.getId(),
                saved.getReviewerId(),
                saved.getModelId(),
                saved.getRating().getValue(),
                saved.getContent(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        );
    }

    public List<ReviewResponse> getReviewsByModel(Long modelId) {
        return reviewRepository.findByModelId(modelId).stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getReviewerId(),
                        r.getModelId(),
                        r.getRating().getValue(),
                        r.getContent(),
                        r.getStatus().name(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
