package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.application.service.UserInfoService;
import com.example.nomodel.member.application.dto.response.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me")
public class UserInfoController {

    private final UserInfoService userInfoService;

    public UserInfoController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    /**
     * 사용자 정보 조회
     * @param userDetails 인증된 사용자 정보
     * @param request HTTP 요청 (JWT 토큰 추출용)
     * @return 사용자 정보
     */
    @GetMapping
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                       HttpServletRequest request) {
        UserInfoResponse response = userInfoService.getUserInfo(userDetails.getMemberId(), request);
        return ResponseEntity.ok().body(ApiUtils.success(response));
    }
}