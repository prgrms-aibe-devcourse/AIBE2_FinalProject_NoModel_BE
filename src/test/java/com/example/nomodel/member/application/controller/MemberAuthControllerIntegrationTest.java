package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.config.TestOAuth2Config;
import com.example.nomodel._core.fixture.TestDataFixture;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Password;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.domain.repository.RefreshTokenRedisRepository;
import com.example.nomodel.member.domain.model.RefreshToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.http.Cookie;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestOAuth2Config.class)
@Transactional
@EnableAutoConfiguration(exclude = {
    ElasticsearchRestClientAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
@DisplayName("MemberAuthController 통합 테스트")
class MemberAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @MockitoBean
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        refreshTokenRedisRepository.deleteAll();
        memberJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 → 로그인 → 토큰재발급 → 로그아웃 전체 플로우 테스트")
    void authenticationFullFlow_Success() throws Exception {
        // 1. 회원가입
        SignUpRequestDto signUpRequest = new SignUpRequestDto("testUser", "test@example.com", "password123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // DB에 회원이 저장되었는지 확인
        assertThat(memberJpaRepository.findByEmail(Email.of("test@example.com"))).isPresent();

        // 2. 로그인
        LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "password123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // 쿠키에서 토큰 추출
        String refreshToken = extractTokenFromCookie(loginResult, "refreshToken");
        String accessToken = extractTokenFromCookie(loginResult, "accessToken");
        
        // Redis에 리프레시 토큰이 저장되었는지 Mock으로 확인
        given(refreshTokenRedisRepository.findByRefreshToken(anyString())).willReturn(createMockRefreshToken(refreshToken));
        assertThat(refreshTokenRedisRepository.findByRefreshToken(refreshToken)).isNotNull();

        // 3. 토큰 재발급
        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // 새로운 액세스 토큰 확인
        String newAccessToken = extractTokenFromCookie(refreshResult, "accessToken");
        assertThat(newAccessToken).isNotBlank();

        // 4. 로그아웃
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 로그아웃 후 쿠키가 삭제되었는지 확인
        // (로그아웃 시 Max-Age=0으로 쿠키 삭제)
    }

    @Test
    @DisplayName("중복 이메일 회원가입 실패")
    void signUp_DuplicateEmail_ShouldFail() throws Exception {
        // given - 기존 회원 생성 (TestDataFixture 사용)
        Member existingMember = TestDataFixture.createMember("existingUser", "existing@example.com", "password123", 
                com.example.nomodel.member.domain.model.Role.USER, 
                com.example.nomodel.member.domain.model.Status.ACTIVE, passwordEncoder);
        memberJpaRepository.save(existingMember);

        // when & then - 동일한 이메일로 회원가입 시도
        SignUpRequestDto signUpRequest = new SignUpRequestDto("newUser", "existing@example.com", "newpassword");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패")
    void login_WrongPassword_ShouldFail() throws Exception {
        // given - 회원 생성 (TestDataFixture 사용)
        Member member = TestDataFixture.createMember("testUser", "test@example.com", "correctPassword", 
                com.example.nomodel.member.domain.model.Role.USER, 
                com.example.nomodel.member.domain.model.Status.ACTIVE, passwordEncoder);
        memberJpaRepository.save(member);

        // when & then - 잘못된 비밀번호로 로그인
        LoginRequestDto loginRequest = new LoginRequestDto("test@example.com", "wrongPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("비활성 회원 로그인 실패")
    void login_SuspendedMember_ShouldFail() throws Exception {
        // given - 비활성 회원 생성 (TestDataFixture 사용)
        Member member = TestDataFixture.createSuspendedMember(passwordEncoder);
        memberJpaRepository.save(member);

        // when & then
        LoginRequestDto loginRequest = new LoginRequestDto("suspended@example.com", "suspendedPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 실패")
    void refreshToken_InvalidToken_ShouldFail() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer invalid-refresh-token"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("토큰 없이 로그아웃 실패")
    void logout_NoToken_ShouldFail() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("유효성 검증 실패 - 잘못된 이메일 형식")
    void signUp_InvalidEmailFormat_ShouldFail() throws Exception {
        // given
        SignUpRequestDto signUpRequest = new SignUpRequestDto("testUser", "invalid-email", "password123");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("유효성 검증 실패 - 짧은 비밀번호")
    void signUp_ShortPassword_ShouldFail() throws Exception {
        // given
        SignUpRequestDto signUpRequest = new SignUpRequestDto("testUser", "test@example.com", "123");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * JSON 응답에서 토큰 값을 추출하는 헬퍼 메소드
     */
    private String extractTokenFromResponse(String jsonResponse, String tokenType) throws Exception {
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        return jsonNode.get("response").get(tokenType).asText();
    }

    /**
     * 쿠키에서 토큰 값을 추출하는 헬퍼 메소드
     */
    private String extractTokenFromCookie(MvcResult result, String cookieName) {
        Cookie[] cookies = result.getResponse().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Mock RefreshToken 생성 헬퍼 메소드
     */
    private RefreshToken createMockRefreshToken(String tokenValue) {
        return RefreshToken.builder()
                .id("1")  // memberId는 숫자여야 함
                .refreshToken(tokenValue)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }
}