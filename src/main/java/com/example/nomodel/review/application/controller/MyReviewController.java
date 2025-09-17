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
    public ResponseEntity<List<ReviewResponse>> getMyAllReviews(
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        List<ReviewResponse> myReviews = reviewService.getMyAllReviews(reviewerId);
        return ResponseEntity.ok(myReviews);
    }

    /**
     * 내가 작성한 특정 리뷰 조회
     */
    @Operation(summary = "내 특정 리뷰 조회", description = "내가 작성한 특정 리뷰를 조회합니다.")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        ReviewResponse myReview = reviewService.getMyReview(reviewerId, reviewId);
        return ResponseEntity.ok(myReview);
    }

    /**
     * 내가 작성한 리뷰 수정
     */
    @Operation(summary = "내 리뷰 수정", description = "내가 작성한 리뷰를 수정합니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId,
            @RequestBody ReviewRequest request
    ) {
        ReviewResponse updated = reviewService.updateMyReview(reviewerId, reviewId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 내가 작성한 리뷰 삭제
     */
    @Operation(summary = "내 리뷰 삭제", description = "내가 작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteMyReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        reviewService.deleteMyReview(reviewerId, reviewId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}

// ReviewService에 추가 필요한 메서드들
/*
public class ReviewService {

    // 내가 작성한 모든 리뷰 조회
    public List<ReviewResponse> getMyAllReviews(Long reviewerId) {
        // reviewerId로 모든 활성 리뷰 조회
        // ReviewStatus.ACTIVE인 것만 필터링
    }

    // 내가 작성한 특정 리뷰 조회 (권한 체크 포함)
    public ReviewResponse getMyReview(Long reviewerId, Long reviewId) {
        // reviewId로 리뷰 조회 후 reviewerId 일치 여부 확인
        // 권한이 없으면 예외 처리
    }

    // 내가 작성한 리뷰 수정 (권한 체크 포함)
    public ReviewResponse updateMyReview(Long reviewerId, Long reviewId, ReviewRequest request) {
        // reviewId로 리뷰 조회 후 reviewerId 일치 여부 확인
        // 권한이 있으면 수정, 없으면 예외 처리
    }

    // 내가 작성한 리뷰 삭제 (권한 체크 포함)
    public void deleteMyReview(Long reviewerId, Long reviewId) {
        // reviewId로 리뷰 조회 후 reviewerId 일치 여부 확인
        // 권한이 있으면 삭제(status 변경), 없으면 예외 처리
    }
}
*/