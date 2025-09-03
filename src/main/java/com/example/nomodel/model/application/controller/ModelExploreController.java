package com.example.nomodel.model.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.model.application.dto.ModelGalleryResponse;
import com.example.nomodel.model.application.service.ModelExploreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/models/explore")
public class ModelExploreController {

    private final ModelExploreService modelExploreService;

    /**
     * 관리자 모델 목록 조회
     * @param userDetails 인증된 사용자 정보
     * @return 공개된 관리자 모델 목록
     */
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminModels(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ModelGalleryResponse response = modelExploreService.getAdminModels();
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    /**
     * 사용자 본인 모델 목록 조회
     * @param userDetails 인증된 사용자 정보
     * @return 사용자가 생성한 모든 모델 목록
     */
    @GetMapping("/my-models")
    public ResponseEntity<?> getMyModels(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ModelGalleryResponse response = modelExploreService.getUserOwnedModels(userDetails.getMemberId());
        return ResponseEntity.ok(ApiUtils.success(response));
    }

    /**
     * 전체 접근 가능한 모델 목록 조회
     * @param userDetails 인증된 사용자 정보
     * @return 사용자가 접근 가능한 모든 모델 목록 (관리자 모델 + 본인 모델)
     */
    @GetMapping("/accessible")
    public ResponseEntity<?> getAccessibleModels(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ModelGalleryResponse response = modelExploreService.getAllAccessibleModels(userDetails.getMemberId());
        return ResponseEntity.ok(ApiUtils.success(response));
    }
}