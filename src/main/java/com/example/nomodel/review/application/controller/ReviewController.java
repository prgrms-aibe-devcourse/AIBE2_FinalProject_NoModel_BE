package com.example.nomodel.review.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.controller.AIModelSearchController;
import com.example.nomodel.model.application.service.AIModelSearchService;
import com.example.nomodel.review.application.dto.request.ReviewRequest;
import com.example.nomodel.review.application.dto.response.ReviewResponse;
import com.example.nomodel.review.application.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
@Tag(name = "Review API", description = "리뷰 관련 API")
@RestController
@RequestMapping("/models/{modelId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class); // 이 줄 추가

    private final ReviewService reviewService;
    private final AIModelSearchService aiModelSearchService;

    @Operation(summary = "리뷰 등록", description = "특정 모델에 리뷰를 등록합니다. modelId는 DB ID(숫자) 또는 Elasticsearch document ID(문자열) 모두 지원합니다.")
    @PostMapping
    public ResponseEntity<?> createReview(
            @PathVariable String modelId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId,
            @RequestBody ReviewRequest request
    ) {
        try {
            Long actualModelId = convertToModelId(modelId);
            ReviewResponse response = reviewService.createReview(reviewerId, actualModelId, request);
            return ResponseEntity.ok(ApiUtils.success(response));
        } catch (Exception e) {
            log.error("리뷰 생성 실패: modelId={}, error={}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiUtils.error("리뷰 생성 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "리뷰 조회", description = "특정 모델에 대한 리뷰 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getReviewsByModel(@PathVariable String modelId) { // String으로 변경
        try {
            Long actualModelId = convertToModelId(modelId);
            List<ReviewResponse> responses = reviewService.getReviewsByModel(actualModelId);
            return ResponseEntity.ok(ApiUtils.success(responses));
        } catch (Exception e) {
            log.error("리뷰 조회 실패: modelId={}, error={}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiUtils.error("리뷰 조회 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "리뷰 수정", description = "특정 리뷰를 수정합니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable String modelId, // String으로 변경
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId,
            @RequestBody ReviewRequest request
    ) {
        try {
            Long actualModelId = convertToModelId(modelId);
            ReviewResponse response = reviewService.updateReview(reviewerId, reviewId, request);
            return ResponseEntity.ok(ApiUtils.success(response));
        } catch (Exception e) {
            log.error("리뷰 수정 실패: reviewId={}, error={}", reviewId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiUtils.error("리뷰 수정 실패: " + e.getMessage()));
        }
    }

    @Operation(summary = "리뷰 삭제", description = "특정 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable String modelId, // String으로 변경
            @PathVariable Long reviewId,
            @AuthenticationPrincipal(expression = "memberId") Long reviewerId
    ) {
        try {
            Long actualModelId = convertToModelId(modelId);
            ReviewResponse deleted = reviewService.deleteReview(reviewerId, reviewId);
            return ResponseEntity.ok(ApiUtils.success(deleted));
        } catch (Exception e) {
            log.error("리뷰 삭제 실패: reviewId={}, error={}", reviewId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiUtils.error("리뷰 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * modelId를 Long 타입으로 변환
     * - 숫자 형태면 DB modelId로 처리
     * - 문자열이면 Elasticsearch document ID로 변환
     */
    private Long convertToModelId(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            throw new IllegalArgumentException("modelId는 필수입니다.");
        }

        // 숫자인지 확인
        if (modelId.matches("\\d+")) {
            log.debug("DB modelId로 처리: {}", modelId);
            return Long.parseLong(modelId);
        } else {
            log.debug("Elasticsearch document ID로 처리: {}", modelId);
            return aiModelSearchService.getModelIdByDocumentId(modelId);
        }
    }
}