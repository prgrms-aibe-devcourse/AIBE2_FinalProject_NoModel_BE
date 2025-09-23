package com.example.nomodel.report.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {

    PENDING("접수", "PENDING"),
    UNDER_REVIEW("검토중", "UNDER_REVIEW"),
    REJECTED("반려", "REJECTED"),
    RESOLVED("해결완료", "RESOLVED");

    private final String description;
    private final String value;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isUnderReview() {
        return this == UNDER_REVIEW;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean isCompleted() {
        return this == REJECTED || this == RESOLVED;
    }
}