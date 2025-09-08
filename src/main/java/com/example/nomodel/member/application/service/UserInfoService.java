package com.example.nomodel.member.application.service;

import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.Role;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.member.application.dto.response.UserInfoResponse;
import com.example.nomodel.point.domain.model.MemberPointBalance;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.subscription.domain.model.MemberSubscription;
import com.example.nomodel.subscription.domain.model.PlanType;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInfoService {

    private final MemberJpaRepository memberRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final MemberPointBalanceRepository memberPointBalanceRepository;

    /**
     * 사용자 정보 조회
     * @param memberId 회원 ID
     * @return 사용자 정보 응답
     */
    public UserInfoResponse getUserInfo(Long memberId) {
        // 회원 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 현재 활성 구독 정보 조회
        String planType = getCurrentPlanType(memberId);

        // 포인트 정보 조회
        Integer points = getCurrentPoints(memberId);

        // 사용자 권한 확인
        String role = member.getRole().name();

        return new UserInfoResponse(
                member.getId(),
                member.getUsername(),
                member.getEmail().getValue(),
                member.getCreatedAt(),
                planType,
                points,
                role
        );
    }

    /**
     * 현재 활성 구독의 플랜 타입 조회
     * @param memberId 회원 ID
     * @return 플랜 타입 문자열 (기본값: "free")
     */
    private String getCurrentPlanType(Long memberId) {
        return memberSubscriptionRepository.findByMemberId(memberId)
                .stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(subscription -> subscription.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .findFirst()
                .map(subscription -> subscription.getSubscription().getPlanType().getValue())
                .orElse(PlanType.FREE.getValue());
    }

    /**
     * 현재 보유 포인트 조회
     * @param memberId 회원 ID
     * @return 보유 포인트 (기본값: 0)
     */
    private Integer getCurrentPoints(Long memberId) {
        return Optional.ofNullable(memberPointBalanceRepository.findById(memberId).orElse(null))
                .map(MemberPointBalance::getAvailablePoints)
                .map(BigDecimal::intValue)
                .orElse(0);
    }
}