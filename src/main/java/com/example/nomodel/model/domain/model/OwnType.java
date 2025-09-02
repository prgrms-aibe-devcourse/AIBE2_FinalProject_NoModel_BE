package com.example.nomodel.model.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OwnType {

    ADMIN("NoModel 자체 제작 모델", "ADMIN"),
    USER("유저 제작 모델", "USER");

    private final String description;
    private final String authority;

    public boolean isAdminOwned() {
        return this == ADMIN;
    }

    public boolean isUserOwned() {
        return this == USER;
    }
}