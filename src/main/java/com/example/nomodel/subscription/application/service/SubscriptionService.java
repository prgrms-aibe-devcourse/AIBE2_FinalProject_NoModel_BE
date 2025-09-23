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
                        s.getPeriod()    //Integer을 Long으로 변환
                ))
                .collect(Collectors.toList());
    }

    public MemberSubscriptionResponse getMySubscription(Long memberId) {
        return domainService.findActiveSubscription(memberId)
                .map(sub -> new MemberSubscriptionResponse(
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
                ))
                .orElse(MemberSubscriptionResponse.empty());
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

    public MemberSubscriptionResponse cancelMySubscription(Long memberId, CancellationReason reason) {
        MemberSubscription cancelled = domainService.cancelActiveSubscription(memberId, reason);

        return new MemberSubscriptionResponse(
                cancelled.getId(),
                cancelled.getMemberId(),
                cancelled.getSubscription().getId(),
                cancelled.getStatus().name(),
                cancelled.getAutoRenewal(),
                cancelled.getStartedAt(),
                cancelled.getExpiresAt(),
                cancelled.getCancelledAt(),
                cancelled.getCancellationReason() != null ? cancelled.getCancellationReason().name() : null,
                cancelled.getPaidAmount()
        );
    }

}
