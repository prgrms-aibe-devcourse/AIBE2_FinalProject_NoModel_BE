package com.example.nomodel.subscription.domain.model;

import lombok.Getter;

@Getter
public enum PlanType {
    FREE("free", "무료 플랜"),
    PRO("pro", "프로 플랜"),
    ENTERPRISE("enterprise", "엔터프라이즈 플랜");

    private final String value;
    private final String displayName;

    PlanType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public static PlanType fromValue(String value) {
        for (PlanType planType : values()) {
            if (planType.value.equals(value)) {
                return planType;
            }
        }
        throw new IllegalArgumentException("Unknown plan type: " + value);
    }
}