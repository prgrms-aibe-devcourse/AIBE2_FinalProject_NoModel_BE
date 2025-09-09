package com.example.nomodel.member.application.controller;

import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.application.service.MemberAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MemberAuthController.class)
@AutoConfigureRestDocs
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화 (REST Docs 테스트용)
@DisplayName("MemberAuthController REST Docs 테스트")
class MemberAuthControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberAuthService memberAuthService;

    @Test
    @DisplayName("회원가입 API 문서화")
    void signUp_Documentation() throws Exception {
        // given
        SignUpRequestDto requestDto = new SignUpRequestDto("testUser", "test@example.com", "password123");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("auth-signup",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("username").type(JsonFieldType.STRING)
                                        .description("사용자명 (2-20자)"),
                                fieldWithPath("email").type(JsonFieldType.STRING)
                                        .description("이메일 주소"),
                                fieldWithPath("password").type(JsonFieldType.STRING)
                                        .description("비밀번호 (4-20자)")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.NULL)
                                        .description("응답 데이터 (회원가입시 null)").optional(),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));

        then(memberAuthService).should().signUp(any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("로그인 API 문서화")
    void login_Documentation() throws Exception {
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
                .andDo(document("auth-login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING)
                                        .description("이메일 주소"),
                                fieldWithPath("password").type(JsonFieldType.STRING)
                                        .description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.STRING)
                                        .description("로그인 성공 메시지"),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("토큰 재발급 API 문서화")
    void refreshToken_Documentation() throws Exception {
        // given
        AuthTokenDTO responseDto = new AuthTokenDTO("Bearer", "new-access-token", 3600000L, "new-refresh-token", 604800000L);

        given(memberAuthService.refreshToken(any())).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer refresh-token-value"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("auth-refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer {refresh-token}")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.STRING)
                                        .description("토큰 재발급 성공 메시지"),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("로그아웃 API 문서화")
    void logout_Documentation() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access-token-value"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("auth-logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer {access-token}")
                        ),
                        responseFields(
                                fieldWithPath("success").type(JsonFieldType.BOOLEAN)
                                        .description("요청 성공 여부"),
                                fieldWithPath("response").type(JsonFieldType.STRING)
                                        .description("로그아웃 성공 메시지"),
                                fieldWithPath("error").type(JsonFieldType.NULL)
                                        .description("에러 정보 (성공시 null)").optional()
                        )
                ));

        then(memberAuthService).should().logout(any());
    }
}