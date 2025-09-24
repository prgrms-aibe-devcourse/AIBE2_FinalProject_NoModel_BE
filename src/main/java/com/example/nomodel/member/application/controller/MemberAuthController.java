package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.application.service.MemberAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    /**
     * 회원 가입
     * @param requestDto 회원가입 요청 정보
     * @param bindingResult 검증 결과
     * @return 회원가입 성공 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequestDto requestDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        memberAuthService.signUp(requestDto);

        return ResponseEntity.ok().body(ApiUtils.success(null));
    }

    /**
     * 로그인
     * @param requestDto 로그인 요청 정보 (이메일, 비밀번호)
     * @param response HTTP 응답 (쿠키 설정용)
     * @return 로그인 성공 응답 (토큰은 쿠키로 전송)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto, HttpServletResponse response) {
        AuthTokenDTO authTokenDTO = memberAuthService.login(requestDto);

        // Access Token 쿠키 설정
        addCookie(response, "accessToken", authTokenDTO.accessToken(), (int) (authTokenDTO.accessTokenValidTime() / 1000));
        addCookie(response, "refreshToken", authTokenDTO.refreshToken(), (int) (authTokenDTO.refreshTokenValidTime() / 1000));

        // 응답에는 토큰 값 제외하고 성공 메시지만 포함
        return ResponseEntity.ok().body(ApiUtils.success("로그인 성공"));
    }

    /**
     * 토큰 재발급
     * @param request HTTP 요청 (쿠키에서 리프레시 토큰 추출)
     * @param response HTTP 응답 (새로운 토큰 쿠키 설정용)
     * @return 토큰 재발급 성공 응답 (새로운 토큰은 쿠키로 전송)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        AuthTokenDTO authTokenDTO = memberAuthService.refreshToken(request);

        // 새로운 Access Token 쿠키 설정
        // Access Token 쿠키 설정
        addCookie(response, "accessToken", authTokenDTO.accessToken(), (int) (authTokenDTO.accessTokenValidTime() / 1000));
        addCookie(response, "refreshToken", authTokenDTO.refreshToken(), (int) (authTokenDTO.refreshTokenValidTime() / 1000));

        return ResponseEntity.ok().body(ApiUtils.success("토큰 재발급 성공"));
    }

    /**
     * 로그아웃
     * @param request HTTP 요청 (쿠키에서 액세스 토큰 추출)
     * @param response HTTP 응답 (쿠키 삭제용)
     * @return 로그아웃 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        memberAuthService.logout(request);

        // Access Token 쿠키 삭제
        addCookie(response, "accessToken", "", 0);
        addCookie(response, "refreshToken", "", 0);

        return ResponseEntity.ok().body(ApiUtils.success("로그아웃 성공"));
    }

    /**
     * 보안이 적용된 토큰 쿠키 생성 헬퍼 메서드 (ResponseCookie 기반)
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAgeInSeconds 만료 시간 (초)
     * @return ResponseCookie
     */
    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeInSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)            // JS 접근 불가
                .secure(false)              // HTTPS 전용 (배포 환경에서는 반드시 true)
                .sameSite("Lax")          // 크로스 도메인 허용
                .path("/")                 // 모든 경로에서 사용 가능
                .maxAge(maxAgeInSeconds)   // 만료 시간 설정
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
