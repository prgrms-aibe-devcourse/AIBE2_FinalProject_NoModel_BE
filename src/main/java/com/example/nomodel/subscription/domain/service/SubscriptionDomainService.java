package com.example.nomodel.subscription.domain.service;

import com.example.nomodel.subscription.domain.model.*;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import com.example.nomodel.subscription.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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

    public MemberSubscription subscribe(Long memberId, Long subscriptionId, BigDecimal paidAmount) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 플랜을 찾을 수 없습니다."));
        MemberSubscription memberSub = new MemberSubscription(memberId, subscription, paidAmount);
        return memberSubscriptionRepository.save(memberSub);
    }

    public List<MemberSubscription> getMemberSubscriptions(Long memberId) {
        return memberSubscriptionRepository.findByMemberId(memberId);
    }

    public void cancel(Long memberSubscriptionId, CancellationReason reason) {
        MemberSubscription memberSub = memberSubscriptionRepository.findById(memberSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 내역을 찾을 수 없습니다."));
        memberSub.cancel(reason);
        memberSubscriptionRepository.save(memberSub);
    }
}
