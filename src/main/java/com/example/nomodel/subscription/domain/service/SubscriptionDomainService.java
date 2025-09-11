package com.example.nomodel.subscription.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.subscription.application.dto.request.SubscriptionRequest;
import com.example.nomodel.subscription.domain.model.*;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import com.example.nomodel.subscription.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionDomainService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;

    public SubscriptionDomainService(SubscriptionRepository subscriptionRepository,
                                     MemberSubscriptionRepository memberSubscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.memberSubscriptionRepository = memberSubscriptionRepository;
    }

    public List<Subscription> getAllPlans() {
        return subscriptionRepository.findAll();
    }

    public MemberSubscription createSubscription(Long memberId, SubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        MemberSubscription memberSubscription = new MemberSubscription(
                memberId,
                subscription,
                request.getPaidAmount()
        );

        return memberSubscriptionRepository.save(memberSubscription);
    }


    public Optional<MemberSubscription> findActiveSubscription(Long memberId) {
        return memberSubscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE);
    }

    public void cancel(Long memberSubscriptionId, CancellationReason reason) {
        MemberSubscription memberSub = memberSubscriptionRepository.findById(memberSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 내역을 찾을 수 없습니다."));
        memberSub.cancel(reason);
        memberSubscriptionRepository.save(memberSub);
    }
}
