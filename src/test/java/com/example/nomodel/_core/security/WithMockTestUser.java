package com.example.nomodel._core.security;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 테스트용 더미 사용자 어노테이션
 * 모든 통합 테스트에서 공통으로 사용할 수 있는 Mock User 설정
 */
@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(
    username = "testuser@example.com",
    authorities = {"ROLE_USER"}
)
public @interface WithMockTestUser {
}