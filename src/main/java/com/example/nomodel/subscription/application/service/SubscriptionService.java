package com.example.nomodel.subscription.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.subscription.application.dto.request.SubscriptionRequest;
import com.example.nomodel.subscription.application.dto.response.MemberSubscriptionResponse;
import com.example.nomodel.subscription.application.dto.response.SubscriptionResponse;
import com.example.nomodel.subscription.domain.model.MemberSubscription;
import com.example.nomodel.subscription.domain.model.Subscription;
import com.example.nomodel.subscription.domain.model.CancellationReason;
import com.example.nomodel.subscription.domain.service.SubscriptionDomainService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionDomainService domainService;

    public SubscriptionService(SubscriptionDomainService domainService) {
        this.domainService = domainService;
    }

    public List<SubscriptionResponse> getPlans() {
        return domainService.getAllPlans().stream()
                .map(s -> new SubscriptionResponse(
                        s.getId(),
                        s.getPlanType(),
                        s.getDescription(),
                        s.getPrice(),
                        s.getPeriod()
                ))
                .collect(Collectors.toList());
    }

    public MemberSubscriptionResponse getMySubscription(Long memberId) {
        MemberSubscription sub = domainService.findActiveSubscription(memberId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        return new MemberSubscriptionResponse(
                sub.getId(),
                sub.getMemberId(),
                sub.getSubscription().getId(),
                sub.getStatus().name(),
                sub.getAutoRenewal(),
                sub.getStartedAt(),
                sub.getExpiresAt(),
                sub.getCancelledAt(),
                sub.getCancellationReason() != null ? sub.getCancellationReason().name() : null,
                sub.getPaidAmount()
        );
    }

    // 구독 생성
    public MemberSubscriptionResponse createSubscription(Long memberId, SubscriptionRequest request) {
        MemberSubscription saved = domainService.createSubscription(memberId, request);

        return new MemberSubscriptionResponse(
                saved.getId(),
                saved.getMemberId(),
                saved.getSubscription().getId(),
                saved.getStatus().name(),
                saved.getAutoRenewal(),
                saved.getStartedAt(),
                saved.getExpiresAt(),
                saved.getCancelledAt(),
                saved.getCancellationReason() != null ? saved.getCancellationReason().name() : null,
                saved.getPaidAmount()
        );
    }




    public void cancel(Long memberSubscriptionId, CancellationReason reason) {
        domainService.cancel(memberSubscriptionId, reason);
    }
}
