package com.example.nomodel.report.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {

    PENDING("접수", "PENDING"),
    UNDER_REVIEW("검토중", "UNDER_REVIEW"),
    ACCEPTED("승인", "ACCEPTED"),
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

    public boolean isAccepted() {
        return this == ACCEPTED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean isCompleted() {
        return this == ACCEPTED || this == REJECTED || this == RESOLVED;
    }
}