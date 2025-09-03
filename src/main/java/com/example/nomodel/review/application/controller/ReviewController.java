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

import java.util.List;

@Tag(name = "Review API", description = "리뷰 관련 API")
@RestController
@RequestMapping("/models/{modelId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    //리뷰 등록 Controller 엔드포인트
    @Operation(summary = "리뷰 등록", description = "특정 모델에 리뷰를 등록합니다.")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long modelId,
            //여기 고침(9/2 :18:46) @AuthenticationPrincipal(expression = "id") ->
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId, // 로그인 사용자 ID
            @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(reviewerId, modelId, request));
    }

    //리뷰 조회 Controller 엔드포인트
    @Operation(summary = "리뷰 조회", description = "특정 모델에 대한 리뷰 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByModel(@PathVariable Long modelId) {
        return ResponseEntity.ok(reviewService.getReviewsByModel(modelId));
    }
}
