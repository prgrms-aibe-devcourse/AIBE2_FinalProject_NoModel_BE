package com.example.nomodel.subscription.domain.repository;

import com.example.nomodel.subscription.domain.model.MemberSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    List<MemberSubscription> findByMemberId(Long memberId);
}
