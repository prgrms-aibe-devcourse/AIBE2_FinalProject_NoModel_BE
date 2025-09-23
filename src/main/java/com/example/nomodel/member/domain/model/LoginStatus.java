package com.example.nomodel.member.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoginStatus {
    SUCCESS("로그인 성공"),
    FAILURE("로그인 실패");

    private final String description;
}