package com.example.nomodel.review.application.controller;

import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.application.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/models/{modelId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ReviewResponse createReview(@PathVariable Long modelId,
                                       @RequestBody ReviewRequest request,
                                       @RequestHeader("X-Member-Id") Long reviewerId) {
        return reviewService.createReview(reviewerId, modelId, request);
    }

    @GetMapping
    public List<ReviewResponse> getReviews(@PathVariable Long modelId) {
        return reviewService.getReviewsByModel(modelId);
    }
}
