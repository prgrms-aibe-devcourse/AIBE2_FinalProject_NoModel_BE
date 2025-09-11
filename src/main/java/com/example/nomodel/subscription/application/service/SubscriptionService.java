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


    public MemberSubscriptionResponse subscribe(Long memberId, SubscriptionRequest request) {
        MemberSubscription sub = domainService.subscribe(
                memberId, request.getSubscriptionId(), request.getPaidAmount()
        );
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

    public void cancel(Long memberSubscriptionId, CancellationReason reason) {
        domainService.cancel(memberSubscriptionId, reason);
    }
}
