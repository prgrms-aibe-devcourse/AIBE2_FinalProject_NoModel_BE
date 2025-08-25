package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.application.service.MemberAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberAuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
@DisplayName("MemberAuthController 단위 테스트")
class MemberAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberAuthService memberAuthService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_Success() throws Exception {
        // given
        SignUpRequestDto requestDto = new SignUpRequestDto("testUser", "test@example.com", "password123");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").doesNotExist());

        then(memberAuthService).should().signUp(any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검증 오류")
    void signUp_ValidationError() throws Exception {
        // given - 잘못된 이메일 형식
        SignUpRequestDto requestDto = new SignUpRequestDto("", "invalid-email", "1");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());

        then(memberAuthService).should(never()).signUp(any());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("test@example.com", "password123");
        AuthTokenDTO responseDto = new AuthTokenDTO("Bearer", "access-token", 3600000L, "refresh-token", 604800000L);

        given(memberAuthService.login(any(LoginRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.grantType").value("Bearer"))
                .andExpect(jsonPath("$.response.accessToken").value("access-token"))
                .andExpect(jsonPath("$.response.refreshToken").value("refresh-token"));

        then(memberAuthService).should().login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 회원")
    void login_MemberNotFound() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("nonexistent@example.com", "password123");

        given(memberAuthService.login(any(LoginRequestDto.class)))
                .willThrow(new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Member not found"));
    }

    @Test
    @DisplayName("로그인 실패 - 유효성 검증 오류")
    void login_ValidationError() throws Exception {
        // given - 빈 이메일과 짧은 비밀번호
        LoginRequestDto requestDto = new LoginRequestDto("", "123");

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        then(memberAuthService).should(never()).login(any());
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_Success() throws Exception {
        // given
        AuthTokenDTO responseDto = new AuthTokenDTO("Bearer", "new-access-token", 3600000L, "new-refresh-token", 604800000L);

        given(memberAuthService.refreshToken(any())).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer refresh-token-value"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response.accessToken").value("new-access-token"));

        then(memberAuthService).should().refreshToken(any());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 없음")
    void refreshToken_TokenNotFound() throws Exception {
        // given
        given(memberAuthService.refreshToken(any()))
                .willThrow(new ApplicationException(ErrorCode.TOKEN_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/auth/refresh"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Token not found in request header"));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer refresh-token-value"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").doesNotExist());

        then(memberAuthService).should().logout(any());
    }

    @Test
    @DisplayName("로그아웃 실패 - 토큰 없음")
    void logout_TokenNotFound() throws Exception {
        // given
        doThrow(new ApplicationException(ErrorCode.TOKEN_NOT_FOUND))
                .when(memberAuthService).logout(any());

        // when & then
        mockMvc.perform(post("/auth/logout"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Token not found in request header"));
    }
}