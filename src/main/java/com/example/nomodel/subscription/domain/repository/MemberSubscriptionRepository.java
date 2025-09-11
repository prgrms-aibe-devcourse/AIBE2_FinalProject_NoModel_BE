package com.example.nomodel.subscription.domain.repository;

import com.example.nomodel.subscription.domain.model.MemberSubscription;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    List<MemberSubscription> findByMemberId(Long memberId);
    Optional<MemberSubscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);
}
