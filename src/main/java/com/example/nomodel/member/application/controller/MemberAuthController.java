package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.application.dto.request.LoginRequestDto;
import com.example.nomodel.member.application.dto.request.SignUpRequestDto;
import com.example.nomodel.member.application.dto.response.AuthTokenDTO;
import com.example.nomodel.member.application.service.MemberAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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
     * @return JWT 토큰 정보
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto requestDto) {
        AuthTokenDTO responseDto = memberAuthService.login(requestDto);
        return ResponseEntity.ok().body(ApiUtils.success(responseDto));
    }

    /**
     * 토큰 재발급
     * @param request HTTP 요청 (Authorization 헤더에서 리프레시 토큰 추출)
     * @return 새로운 JWT 토큰 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        AuthTokenDTO responseDto = memberAuthService.refreshToken(request);
        return ResponseEntity.ok().body(ApiUtils.success(responseDto));
    }

    /**
     * 로그아웃
     * @param request HTTP 요청 (Authorization 헤더에서 액세스 토큰 추출)
     * @return 로그아웃 성공 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        memberAuthService.logout(request);
        return ResponseEntity.ok().body(ApiUtils.success(null));
    }
}