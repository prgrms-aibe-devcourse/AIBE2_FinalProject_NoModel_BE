package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.application.dto.request.SignUpDto;
import com.example.nomodel.member.application.service.MemberAuthService;
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
@RequestMapping("/api/auth")
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    /**
     * 회원 가입
     * @param requestDto 회원가입 요청 정보
     * @param bindingResult 검증 결과
     * @return 회원가입 성공 응답
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpDto requestDto, BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            throw new ApplicationException(ErrorCode.INVALID_REQUEST);
        }

        memberAuthService.signUp(requestDto);

        return ResponseEntity.ok().body(ApiUtils.success(null));
    }
}