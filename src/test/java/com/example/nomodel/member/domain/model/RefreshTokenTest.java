package com.example.nomodel.member.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RefreshToken 도메인 단위 테스트")
class RefreshTokenTest {

    @Test
    @DisplayName("RefreshToken 생성 시 모든 필드가 올바르게 설정된다")
    void createRefreshToken_ShouldSetAllFields() {
        // given
        String memberId = "123";
        String tokenValue = "refresh-token-value";
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        );

        // when
        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .authorities(authorities)
                .refreshToken(tokenValue)
                .build();

        // then
        assertThat(refreshToken.getId()).isEqualTo(memberId);
        assertThat(refreshToken.getRefreshToken()).isEqualTo(tokenValue);
        assertThat(refreshToken.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("RefreshToken 빌더 패턴으로 생성할 수 있다")
    void createRefreshTokenWithBuilder_ShouldWork() {
        // given
        String memberId = "456";
        String tokenValue = "another-refresh-token";
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_USER")
        );

        // when
        RefreshToken refreshToken = RefreshToken.builder()
                .id(memberId)
                .refreshToken(tokenValue)
                .authorities(authorities)
                .build();

        // then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.getId()).isEqualTo(memberId);
        assertThat(refreshToken.getRefreshToken()).isEqualTo(tokenValue);
        assertThat(refreshToken.getAuthorities()).hasSize(2);
        assertThat(refreshToken.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("RefreshToken의 권한 정보가 올바르게 저장된다")
    void refreshToken_ShouldStoreAuthoritiesCorrectly() {
        // given
        List<GrantedAuthority> multipleAuthorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_MANAGER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        // when
        RefreshToken refreshToken = RefreshToken.builder()
                .id("789")
                .refreshToken("multi-authority-token")
                .authorities(multipleAuthorities)
                .build();

        // then
        assertThat(refreshToken.getAuthorities()).hasSize(3);
        assertThat(refreshToken.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "ROLE_MANAGER", "ROLE_ADMIN");
    }
}