package com.example.nomodel.review.application.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리뷰 관련 이벤트
 */
@Getter
@RequiredArgsConstructor
public class ReviewEvent {
    private final Long modelId;
    private final Long reviewId;
    private final String action;  // CREATED, UPDATED, DELETED
    private final Double rating;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public static ReviewEvent created(Long modelId, Long reviewId, Double rating) {
        return new ReviewEvent(modelId, reviewId, "CREATED", rating);
    }

    public static ReviewEvent updated(Long modelId, Long reviewId, Double rating) {
        return new ReviewEvent(modelId, reviewId, "UPDATED", rating);
    }

    public static ReviewEvent deleted(Long modelId, Long reviewId) {
        return new ReviewEvent(modelId, reviewId, "DELETED", null);
    }
}