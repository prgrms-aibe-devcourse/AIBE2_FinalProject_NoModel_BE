package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("LoginHistoryRepository 테스트")
class LoginHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Test
    @DisplayName("성공한 로그인 이력 존재 여부 확인 - 존재함")
    void existsByMemberIdAndLoginStatus_Success_Exists() {
        // given - 테스트용 회원 생성
        Member member = createTestMember();
        LoginHistory successHistory = LoginHistory.createSuccessHistory(member, "hashed-ip");
        entityManager.persist(successHistory);
        entityManager.flush();

        // when
        boolean exists = loginHistoryRepository.existsByMemberIdAndLoginStatus(member.getId(), LoginStatus.SUCCESS);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("성공한 로그인 이력 존재 여부 확인 - 존재하지 않음")
    void existsByMemberIdAndLoginStatus_Success_NotExists() {
        // given - 테스트용 회원 생성 (로그인 이력 없음)
        Member member = createTestMember();

        // when
        boolean exists = loginHistoryRepository.existsByMemberIdAndLoginStatus(member.getId(), LoginStatus.SUCCESS);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("실패한 로그인 이력만 있는 경우 - 성공한 이력 없음")
    void existsByMemberIdAndLoginStatus_OnlyFailures() {
        // given - 테스트용 회원과 실패 이력 생성
        Member member = createTestMember();
        LoginHistory failureHistory = LoginHistory.createFailureHistory("hashed-ip", "Login failed");
        failureHistory.setMember(member);
        entityManager.persist(failureHistory);
        entityManager.flush();

        // when
        boolean exists = loginHistoryRepository.existsByMemberIdAndLoginStatus(member.getId(), LoginStatus.SUCCESS);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("성공과 실패 이력이 모두 있는 경우 - 성공한 이력 존재")
    void existsByMemberIdAndLoginStatus_BothSuccessAndFailure() {
        // given - 테스트용 회원과 성공/실패 이력 모두 생성
        Member member = createTestMember();
        
        LoginHistory failureHistory = LoginHistory.createFailureHistory("hashed-ip", "Login failed");
        failureHistory.setMember(member);
        entityManager.persist(failureHistory);
        
        LoginHistory successHistory = LoginHistory.createSuccessHistory(member, "hashed-ip");
        entityManager.persist(successHistory);
        entityManager.flush();

        // when
        boolean exists = loginHistoryRepository.existsByMemberIdAndLoginStatus(member.getId(), LoginStatus.SUCCESS);

        // then
        assertThat(exists).isTrue();
    }

    private Member createTestMember() {
        Email email = Email.of("test@example.com");
        // Create a simple PasswordEncoder for testing
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded-" + rawPassword;
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encode(rawPassword).equals(encodedPassword);
            }
        };
        Password password = Password.encode("password123", passwordEncoder);
        Member member = Member.createMember("testUser", email, password);
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }
}