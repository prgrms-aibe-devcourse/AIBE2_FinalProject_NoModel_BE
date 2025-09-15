package com.example.nomodel.member.application.service;

import com.example.nomodel._core.base.BaseIntegrationTest;
import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.domain.model.*;
import com.example.nomodel.member.domain.repository.FirstLoginRedisRepository;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * MemberAuthService 통합 테스트
 * TestContainers를 사용하여 실제 MySQL, Redis, ElasticSearch와 연동합니다.
 */
@DisplayName("MemberAuthService 통합 테스트 - MySQL + Redis + ElasticSearch")
@ActiveProfiles("itest")
class MemberAuthServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private FirstLoginRedisRepository firstLoginRedisRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void setUp() {
        // Redis 캐시 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("회원가입 후 Redis에 최초 로그인 상태가 저장된다")
    void signUp_SavesFirstLoginStatusToRedis() {
        // given
        SignUpRequestDto requestDto = new SignUpRequestDto(
                "testUser", "test@example.com", "password123"
        );

        // when
        memberAuthService.signUp(requestDto);

        // then
        Member savedMember = memberJpaRepository.findByEmail(Email.of("test@example.com"))
                .orElseThrow(() -> new AssertionError("회원이 저장되지 않았습니다."));
        
        // Redis에 최초 로그인 상태가 true로 저장되어야 함
        Boolean isFirstLogin = firstLoginRedisRepository.isFirstLogin(savedMember.getId());
        assertThat(isFirstLogin).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 예외가 발생한다")
    @Sql("/sql/basic-members.sql")
    void signUp_DuplicateEmail_ThrowsException() {
        // given - SQL 파일에서 이미 존재하는 이메일
        SignUpRequestDto requestDto = new SignUpRequestDto(
                "duplicateUser", "normal@test.com", "password123"
        );

        // when & then
        assertThatThrownBy(() -> memberAuthService.signUp(requestDto))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("정상적인 로그인이 성공한다")
    @Sql("/sql/basic-members.sql")
    void login_ValidCredentials_Success() {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("normal@test.com", "password123");

        // when
        AuthTokenDTO result = memberAuthService.login(requestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.grantType()).isEqualTo("Bearer");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.accessTokenValidTime()).isPositive();
        assertThat(result.refreshTokenValidTime()).isPositive();
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 로그인 시 예외가 발생한다")
    void login_NonExistentUser_ThrowsException() {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("nonexistent@test.com", "password123");

        // when & then
        assertThatThrownBy(() -> memberAuthService.login(requestDto))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외가 발생한다")
    @Sql("/sql/basic-members.sql")
    void login_WrongPassword_ThrowsException() {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("normal@test.com", "wrongPassword");

        // when & then
        assertThatThrownBy(() -> memberAuthService.login(requestDto))
                .isInstanceOf(Exception.class); // Spring Security BadCredentialsException
    }

    @Test
    @DisplayName("통합 테스트: 회원가입 → MySQL 저장 → Redis 캐시 → 로그인 성공")
    void fullIntegrationTest_SignUpAndLogin() {
        // given
        String email = "integration@test.com";
        String password = "password123";
        SignUpRequestDto signUpRequest = new SignUpRequestDto("integrationUser", email, password);

        // when - 회원가입 (MySQL + Redis)
        memberAuthService.signUp(signUpRequest);

        // then - MySQL에 회원 저장 확인
        Member savedMember = memberJpaRepository.findByEmail(Email.of(email))
                .orElseThrow(() -> new AssertionError("MySQL에 회원이 저장되지 않았습니다."));
        
        assertThat(savedMember.getEmail().getValue()).isEqualTo(email);
        assertThat(savedMember.getUsername()).isEqualTo("integrationUser");
        assertThat(savedMember.getRole()).isEqualTo(Role.USER);
        assertThat(savedMember.getStatus()).isEqualTo(Status.ACTIVE);

        // then - Redis에 최초 로그인 상태 저장 확인
        Boolean isFirstLogin = firstLoginRedisRepository.isFirstLogin(savedMember.getId());
        assertThat(isFirstLogin).isTrue();

        // when - 로그인 (MySQL 인증 + Redis 체크)
        LoginRequestDto loginRequest = new LoginRequestDto(email, password);
        AuthTokenDTO authResult = memberAuthService.login(loginRequest);

        // then - 로그인 성공 확인
        assertThat(authResult).isNotNull();
        assertThat(authResult.grantType()).isEqualTo("Bearer");
        assertThat(authResult.accessToken()).isNotBlank();
        assertThat(authResult.refreshToken()).isNotBlank();
        assertThat(authResult.accessTokenValidTime()).isPositive();
        assertThat(authResult.refreshTokenValidTime()).isPositive();
    }

    @Test
    @DisplayName("Redis 연결 상태 확인 테스트")
    void testRedisConnection() {
        // given
        String testKey = "test:connection";
        String testValue = "testValue";

        // when
        redisTemplate.opsForValue().set(testKey, testValue);
        String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);

        // then
        assertThat(retrievedValue).isEqualTo(testValue);

        // cleanup
        redisTemplate.delete(testKey);
    }

    @Test
    @DisplayName("MySQL 트랜잭션 롤백 테스트")
    @Transactional
    void testMySQLTransactionRollback() {
        // given
        String email = "rollback@test.com";
        SignUpRequestDto requestDto = new SignUpRequestDto("rollbackUser", email, "password123");

        // when
        memberAuthService.signUp(requestDto);

        // then - 트랜잭션 내에서는 데이터 존재
        assertThat(memberJpaRepository.findByEmail(Email.of(email))).isPresent();

        // 메서드 종료 후 @Transactional에 의해 자동 롤백됨
    }

    @Test
    @DisplayName("여러 회원 생성 후 Redis 캐시 상태 확인")
    void testMultipleMembersRedisCache() {
        // given
        SignUpRequestDto user1 = new SignUpRequestDto("user1", "user1@test.com", "password123");
        SignUpRequestDto user2 = new SignUpRequestDto("user2", "user2@test.com", "password123");

        // when
        memberAuthService.signUp(user1);
        memberAuthService.signUp(user2);

        // then
        Member member1 = memberJpaRepository.findByEmail(Email.of("user1@test.com")).orElseThrow();
        Member member2 = memberJpaRepository.findByEmail(Email.of("user2@test.com")).orElseThrow();

        // 두 회원 모두 Redis에 최초 로그인 상태가 true로 저장되어야 함
        assertThat(firstLoginRedisRepository.isFirstLogin(member1.getId())).isTrue();
        assertThat(firstLoginRedisRepository.isFirstLogin(member2.getId())).isTrue();

        // Redis에서 데이터 직접 확인
        String redisKey1 = "first_login:" + member1.getId();
        String redisKey2 = "first_login:" + member2.getId();
        
        assertThat(redisTemplate.opsForValue().get(redisKey1)).isEqualTo("true");
        assertThat(redisTemplate.opsForValue().get(redisKey2)).isEqualTo("true");
    }
}