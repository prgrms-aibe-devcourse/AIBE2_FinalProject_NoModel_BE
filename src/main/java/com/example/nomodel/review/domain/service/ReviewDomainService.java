package com.example.nomodel.review.domain.service;

import com.example.nomodel.review.domain.model.ModelReview;
import com.example.nomodel.review.domain.model.Rating;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewDomainService {

    private final ReviewRepository reviewRepository;

    public ReviewDomainService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ModelReview createReview(Long reviewerId, Long modelId, Integer rating, String content) {
        ModelReview review = new ModelReview(reviewerId, modelId, new Rating(rating), content);
        return reviewRepository.save(review);
    }

    public List<ModelReview> getReviewsByModel(Long modelId) {
        return reviewRepository.findByModelId(modelId);
    }

    public List<ModelReview> getReviewsByReviewer(Long reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId);
    }
}
