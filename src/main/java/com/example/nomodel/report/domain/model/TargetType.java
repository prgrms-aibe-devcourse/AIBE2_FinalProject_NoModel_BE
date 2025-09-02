package com.example.nomodel.report.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetType {

    MODEL("모델", "MODEL"),
    REVIEW("리뷰", "REVIEW");

    private final String description;
    private final String value;

    public boolean isModelReport() {
        return this == MODEL;
    }

    public boolean isReviewReport() {
        return this == REVIEW;
    }
}