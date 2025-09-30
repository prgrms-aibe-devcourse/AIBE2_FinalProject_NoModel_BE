package com.example.nomodel.member.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.member.application.service.UserInfoService;
import com.example.nomodel.member.application.dto.response.UserInfoResponse;
import com.example.nomodel.member.application.dto.response.UserModelStatsResponse;
import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * @return 사용자 정보
     */
    @GetMapping
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserInfoResponse response = userInfoService.getUserInfo(userDetails.getMemberId());
        return ResponseEntity.ok().body(ApiUtils.success(response));
    }

    @GetMapping("/models")
    public ResponseEntity<?> getUserModels(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AIModelDetailResponse> models = userInfoService.getMyModels(userDetails.getMemberId());
        return ResponseEntity.ok().body(ApiUtils.success(models));
    }

    @GetMapping("/models/stats")
    public ResponseEntity<?> getUserModelStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserModelStatsResponse stats = userInfoService.getMyModelStats(userDetails.getMemberId());
        return ResponseEntity.ok().body(ApiUtils.success(stats));
    }
}
