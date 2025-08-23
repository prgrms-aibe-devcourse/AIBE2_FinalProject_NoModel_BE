package com.example.nomodel.member.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Member 도메인 단위 테스트")
class MemberTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("회원 생성 시 기본 역할과 상태가 설정된다")
    void createMember_ShouldSetDefaultRoleAndStatus() {
        // given
        String username = "testUser";
        Email email = Email.of("test@example.com");
        Password password = Password.encode("password123", passwordEncoder);

        // when
        Member member = Member.createMember(username, email, password);

        // then
        assertThat(member.getUsername()).isEqualTo(username);
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.getPassword()).isEqualTo(password);
        assertThat(member.getRole()).isEqualTo(Role.USER);
        assertThat(member.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DisplayName("비밀번호 검증 - 올바른 비밀번호")
    void validatePassword_WithCorrectPassword_ShouldReturnTrue() {
        // given
        String rawPassword = "password123";
        Email email = Email.of("test@example.com");
        Password password = Password.encode(rawPassword, passwordEncoder);
        Member member = Member.createMember("testUser", email, password);

        // when
        boolean isValid = member.validatePassword(rawPassword, passwordEncoder);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("비밀번호 검증 - 잘못된 비밀번호")
    void validatePassword_WithIncorrectPassword_ShouldReturnFalse() {
        // given
        Email email = Email.of("test@example.com");
        Password password = Password.encode("password123", passwordEncoder);
        Member member = Member.createMember("testUser", email, password);

        // when
        boolean isValid = member.validatePassword("wrongPassword", passwordEncoder);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("회원 상태 확인 - 활성 상태")
    void isActive_WithActiveStatus_ShouldReturnTrue() {
        // given
        Email email = Email.of("test@example.com");
        Password password = Password.encode("password123", passwordEncoder);
        Member member = Member.createMember("testUser", email, password);

        // when & then
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("회원 상태 확인 - 비활성 상태")
    void isActive_WithSuspendedStatus_ShouldReturnFalse() {
        // given
        Email email = Email.of("test@example.com");
        Password password = Password.encode("password123", passwordEncoder);
        Member member = Member.createMember("testUser", email, password);
        
        // when
        member.deactivate();
        
        // then
        assertThat(member.isActive()).isFalse();
        assertThat(member.getStatus()).isEqualTo(Status.SUSPENDED);
    }
}