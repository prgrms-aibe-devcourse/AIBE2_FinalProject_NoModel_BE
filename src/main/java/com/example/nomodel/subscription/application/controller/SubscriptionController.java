package com.example.nomodel.subscription.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.subscription.application.dto.request.SubscriptionRequest;
import com.example.nomodel.subscription.application.dto.response.MemberSubscriptionResponse;
import com.example.nomodel.subscription.application.dto.response.SubscriptionResponse;
import com.example.nomodel.subscription.application.service.SubscriptionService;
import com.example.nomodel.subscription.domain.model.CancellationReason;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public ApiUtils.ApiResult<List<SubscriptionResponse>> getPlans() {
        return ApiUtils.success(subscriptionService.getPlans());
    }

    @GetMapping
    public ApiUtils.ApiResult<MemberSubscriptionResponse> getMySubscription(
            @RequestHeader("X-Member-Id") Long memberId) {
        return ApiUtils.success(subscriptionService.getMySubscription(memberId));
    }

    @PostMapping
    public MemberSubscriptionResponse subscribe(@RequestHeader("X-Member-Id") Long memberId,
                                                @RequestBody SubscriptionRequest request) {
        return subscriptionService.subscribe(memberId, request);
    }

    @DeleteMapping("/{memberSubscriptionId}")
    public void cancel(@PathVariable Long memberSubscriptionId,
                       @RequestParam CancellationReason reason) {
        subscriptionService.cancel(memberSubscriptionId, reason);
    }
}
