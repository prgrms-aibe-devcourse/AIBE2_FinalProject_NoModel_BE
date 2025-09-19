package com.example.nomodel.subscription.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.subscription.application.dto.request.SubscriptionRequest;
import com.example.nomodel.subscription.application.dto.response.MemberSubscriptionResponse;
import com.example.nomodel.subscription.application.dto.response.SubscriptionResponse;
import com.example.nomodel.subscription.application.service.SubscriptionService;
import com.example.nomodel.subscription.domain.model.CancellationReason;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // 구독 상품 목록 조회
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionResponse>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getPlans());
    }

    // 내 구독 조회
    @GetMapping
    public ResponseEntity<MemberSubscriptionResponse> getMySubscription(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(user.getMemberId()));
    }

    // 구독 생성
    @PostMapping
    public ResponseEntity<MemberSubscriptionResponse> createSubscription(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody SubscriptionRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.createSubscription(user.getMemberId(), request));
    }

    // 구독 해지
    @DeleteMapping
    public ResponseEntity<MemberSubscriptionResponse> cancelMySubscription(
            @RequestParam CancellationReason reason,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        MemberSubscriptionResponse response = subscriptionService.cancelMySubscription(user.getMemberId(), reason);
        return ResponseEntity.ok(response);
    }
}
