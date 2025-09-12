package com.example.nomodel.subscription.domain.repository;

import com.example.nomodel.subscription.domain.model.MemberSubscription;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    // 특정 회원의 ACTIVE 구독 찾기 (중복 구독 방지 용도)
    Optional<MemberSubscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);
    // 만료일이 지난 ACTIVE 구독들 조회
    List<MemberSubscription> findByStatusAndExpiresAtBefore(SubscriptionStatus status, LocalDateTime now);
    // 특정 회원의 모든 구독 조회 (히스토리 용도)
    List<MemberSubscription> findByMemberId(Long memberId);

}
