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
    // 리뷰 삭제 Controller 엔드포인트
    @Operation(summary = "리뷰 삭제", description = "특정 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiUtils.ApiResult<ReviewResponse>> deleteReview(
            @PathVariable Long modelId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        ReviewResponse deleted = reviewService.deleteReview(reviewerId, reviewId);
        return ResponseEntity.ok(ApiUtils.success(deleted));
    }

    // 새로 추가된 내 리뷰 관리 API들 (ResponseEntity 사용)

    /**
     * 특정 모델에서 내가 작성한 리뷰 조회
     */
    @Operation(summary = "내 리뷰 조회", description = "특정 모델에서 내가 작성한 리뷰를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ReviewResponse> getMyReview(
            @PathVariable Long modelId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        ReviewResponse myReview = reviewService.getMyReviewByModel(reviewerId, modelId);
        return ResponseEntity.ok(myReview);
    }

    /**
     * 특정 모델에서 내 리뷰 수정
     */
    @Operation(summary = "내 리뷰 수정", description = "특정 모델에서 내가 작성한 리뷰를 수정합니다.")
    @PutMapping("/me")
    public ResponseEntity<ReviewResponse> updateMyReview(
            @PathVariable Long modelId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId,
            @RequestBody ReviewRequest request
    ) {
        ReviewResponse updated = reviewService.updateMyReviewByModel(reviewerId, modelId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 특정 모델에서 내 리뷰 삭제
     */
    @Operation(summary = "내 리뷰 삭제", description = "특정 모델에서 내가 작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyReview(
            @PathVariable Long modelId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        reviewService.deleteMyReviewByModel(reviewerId, modelId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}

// 추가로 필요한 ReviewService 메서드들 (구현 필요)
/*
public class ReviewService {

    // 기존 메서드들...

    public ReviewResponse getMyReviewByModel(Long reviewerId, Long modelId) {
        // reviewerId와 modelId로 내 리뷰 조회
        // 리뷰가 없으면 적절한 예외 처리
    }

    public ReviewResponse updateMyReviewByModel(Long reviewerId, Long modelId, ReviewRequest request) {
        // reviewerId와 modelId로 내 리뷰 찾아서 수정
        // 권한 체크 및 존재 여부 확인
    }

    public ReviewResponse deleteMyReviewByModel(Long reviewerId, Long modelId) {
        // reviewerId와 modelId로 내 리뷰 찾아서 삭제 (status 변경)
        // 권한 체크 및 존재 여부 확인
    }
}
*/
