package com.example.nomodel.subscription.application.controller;

import com.example.nomodel.subscription.application.dto.request.SubscriptionRequest;
import com.example.nomodel.subscription.application.dto.response.MemberSubscriptionResponse;
import com.example.nomodel.subscription.application.dto.response.SubscriptionResponse;
import com.example.nomodel.subscription.application.service.SubscriptionService;
import com.example.nomodel.subscription.domain.model.CancellationReason;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members/me/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public List<SubscriptionResponse> getPlans() {
        return subscriptionService.getPlans();
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
