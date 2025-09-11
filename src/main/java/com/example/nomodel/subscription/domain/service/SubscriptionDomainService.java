package com.example.nomodel.subscription.domain.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.subscription.application.dto.request.SubscriptionRequest;
import com.example.nomodel.subscription.application.dto.response.MemberSubscriptionResponse;
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
        // 1. 이미 ACTIVE 구독이 있는지 확인
        Optional<MemberSubscription> activeSub = memberSubscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE);
        if (activeSub.isPresent()) {
            throw new ApplicationException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
        }

        // 2. 구독 상품 조회
        Subscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 3. 새 구독 생성
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

    public MemberSubscription cancelActiveSubscription(Long memberId, CancellationReason reason) {
        MemberSubscription subscription = memberSubscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ApplicationException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscription.cancel(reason);
        return memberSubscriptionRepository.save(subscription);
    }
}

