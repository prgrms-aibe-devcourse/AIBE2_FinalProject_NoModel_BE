package com.example.nomodel.member.domain.model;

import lombok.Getter;

@Getter
public enum Status {
    ACTIVE("ACTIVE", "활성 사용자"),
    INACTIVE("INACTIVE", "비활성 사용자"),
    SUSPENDED("SUSPENDED", "정지된 사용자"),
    BANNED("BANNED", "접근 금지된 사용자");

    private final String code;
    private final String description;

    Status(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String toString() {
        return code;
    }
}