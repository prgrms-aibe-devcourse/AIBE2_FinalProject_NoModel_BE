package com.example.nomodel.review.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
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
    public ResponseEntity<ApiUtils.ApiResult<ReviewResponse>> createReview(
            @PathVariable Long modelId,
            //여기 고침(9/2 :18:46) @AuthenticationPrincipal(expression = "id") ->
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId, // 로그인 사용자 ID
            @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(ApiUtils.success(reviewService.createReview(reviewerId, modelId, request)));
    }

    //리뷰 조회 Controller 엔드포인트
    @Operation(summary = "리뷰 조회", description = "특정 모델에 대한 리뷰 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiUtils.ApiResult<List<ReviewResponse>>> getReviewsByModel(@PathVariable Long modelId) {
        return ResponseEntity.ok(ApiUtils.success(reviewService.getReviewsByModel(modelId)));
    }

    //리뷰 수정 Controller 엔드포인트
    @Operation(summary = "리뷰 수정", description = "특정 리뷰를 수정합니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity <ApiUtils.ApiResult<ReviewResponse>> updateReview(
            @PathVariable Long modelId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId,
            @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(ApiUtils.success(reviewService.updateReview(reviewerId, reviewId, request)));
    }

    //리뷰 삭제 Controller 엔드포인트
    @Operation(summary = "리뷰 삭제", description = "특정 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiUtils.ApiResult<Void>> deleteReview(
            @PathVariable Long modelId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        reviewService.deleteReview(reviewerId, reviewId);
        return ResponseEntity.ok(ApiUtils.success(null));
    }



}
