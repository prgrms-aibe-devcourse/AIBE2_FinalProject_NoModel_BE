package com.example.nomodel.file.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RelationType {

    MODEL("모델", "MODEL"),
    REVIEW("리뷰", "REVIEW"),
    PROFILE("프로필", "PROFILE"),
    AD("광고", "AD"),
    AD_RESULT("광고 결과물", "AD_RESULT");

    private final String description;
    private final String value;

    public boolean isModelRelated() {
        return this == MODEL;
    }

    public boolean isReviewRelated() {
        return this == REVIEW;
    }

    public boolean isProfileRelated() {
        return this == PROFILE;
    }

    public boolean isAdRelated() {
        return this == AD;
    }

    public boolean isAdResultRelated() {
        return this == AD_RESULT;
    }
}