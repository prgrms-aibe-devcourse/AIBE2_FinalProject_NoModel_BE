package com.example.nomodel.review.application.controller;

import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.application.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review API", description = "리뷰 관련 API")
@RestController
@RequestMapping("/models/{modelId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 등록", description = "특정 모델에 리뷰를 등록합니다.")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long modelId,
            @AuthenticationPrincipal(expression = "id") Long reviewerId, // 로그인 사용자 ID
            @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(reviewerId, modelId, request));
    }
}
