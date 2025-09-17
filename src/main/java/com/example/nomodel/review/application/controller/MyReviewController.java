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

@Tag(name = "My Reviews API", description = "내가 작성한 리뷰 관리 API")
@RestController
@RequestMapping("/me/reviews")
@RequiredArgsConstructor
public class MyReviewController {

    private final ReviewService reviewService;

    /**
     * 내가 작성한 모든 리뷰 조회
     */
    @Operation(summary = "내가 작성한 모든 리뷰 조회", description = "로그인한 사용자가 작성한 모든 리뷰를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiUtils.ApiResult<List<ReviewResponse>>> getMyAllReviews(
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        List<ReviewResponse> myReviews = reviewService.getMyAllReviews(reviewerId);
        return ResponseEntity.ok(ApiUtils.success(myReviews));
    }

    /**
     * 내가 작성한 특정 리뷰 조회
     */
    @Operation(summary = "내 특정 리뷰 조회", description = "내가 작성한 특정 리뷰를 조회합니다.")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiUtils.ApiResult<ReviewResponse>> getMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        ReviewResponse myReview = reviewService.getMyReview(reviewerId, reviewId);
        return ResponseEntity.ok(ApiUtils.success(myReview));
    }

    /**
     * 내가 작성한 리뷰 수정
     */
    @Operation(summary = "내 리뷰 수정", description = "내가 작성한 리뷰를 수정합니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiUtils.ApiResult<ReviewResponse>> updateMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId,
            @RequestBody ReviewRequest request
    ) {
        ReviewResponse updated = reviewService.updateMyReview(reviewerId, reviewId, request);
        return ResponseEntity.ok(ApiUtils.success(updated));
    }

    /**
     * 내가 작성한 리뷰 삭제
     */
    @Operation(summary = "내 리뷰 삭제", description = "내가 작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiUtils.ApiResult<Void>> deleteMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        reviewService.deleteMyReview(reviewerId, reviewId);
        // 204 대신 200 OK와 success 메시지 반환
        return ResponseEntity.ok(ApiUtils.success(null));
    }
}