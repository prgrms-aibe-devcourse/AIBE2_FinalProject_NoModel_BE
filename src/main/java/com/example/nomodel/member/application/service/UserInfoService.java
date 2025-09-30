package com.example.nomodel.member.application.service;

import com.example.nomodel.member.application.dto.response.UserInfoResponse;
import com.example.nomodel.member.application.dto.response.UserModelStatsResponse;
import com.example.nomodel.member.domain.model.Member;
import com.example.nomodel.member.domain.model.LoginStatus;
import com.example.nomodel.member.domain.repository.FirstLoginRedisRepository;
import com.example.nomodel.member.domain.repository.LoginHistoryRepository;
import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.command.application.dto.AIModelDetailResponse;
import com.example.nomodel.model.command.domain.repository.AIModelJpaRepository;
import com.example.nomodel.model.command.domain.repository.AdResultJpaRepository;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.model.query.service.AIModelDetailFacadeService;
import com.example.nomodel.point.domain.model.MemberPointBalance;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.review.domain.model.ReviewStatus;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.subscription.domain.model.PlanType;
import com.example.nomodel.subscription.domain.model.SubscriptionStatus;
import com.example.nomodel.subscription.domain.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInfoService {

    private final MemberJpaRepository memberRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final MemberPointBalanceRepository memberPointBalanceRepository;
    private final AIModelJpaRepository aiModelRepository;
    private final AdResultJpaRepository adResultRepository;
    private final ModelStatisticsJpaRepository modelStatisticsRepository;
    private final ReviewRepository reviewRepository;
    private final AIModelDetailFacadeService modelDetailFacadeService;
    private final FirstLoginRedisRepository firstLoginRedisRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    public UserInfoResponse getUserInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        String planType = getCurrentPlanType(memberId);
        Integer points = getCurrentPoints(memberId);
        String role = member.getRole().name();
        Long modelCount = getModelCount(memberId);
        Long projectCount = getProjectCount(memberId);
        Boolean isFirstLogin = checkFirstLoginStatus(memberId);

        return new UserInfoResponse(
                member.getId(),
                member.getUsername(),
                member.getEmail().getValue(),
                member.getCreatedAt(),
                planType,
                points,
                role,
                modelCount,
                projectCount,
                isFirstLogin
        );
    }

    private String getCurrentPlanType(Long memberId) {
        return memberSubscriptionRepository.findByMemberId(memberId)
                .stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(subscription -> subscription.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .findFirst()
                .map(subscription -> subscription.getSubscription().getPlanType().getValue())
                .orElse(PlanType.FREE.getValue());
    }

    private Integer getCurrentPoints(Long memberId) {
        return memberPointBalanceRepository.findById(memberId)
                .map(MemberPointBalance::getAvailablePoints)
                .map(BigDecimal::intValue)
                .orElse(0);
    }

    private Long getModelCount(Long memberId) {
        return aiModelRepository.countByOwnerId(memberId);
    }

    private Long getProjectCount(Long memberId) {
        return adResultRepository.countByMemberId(memberId);
    }

    private Boolean checkFirstLoginStatus(Long memberId) {
        Boolean cachedResult = firstLoginRedisRepository.isFirstLogin(memberId);
        if (cachedResult != null) {
            return cachedResult;
        }

        boolean isFirstLogin = !loginHistoryRepository.existsByMemberIdAndLoginStatus(memberId, LoginStatus.SUCCESS);
        firstLoginRedisRepository.setFirstLoginStatus(memberId, false);
        return isFirstLogin;
    }

    public UserModelStatsResponse getMyModelStats(Long memberId) {
        Long totalModelCount = aiModelRepository.countByOwnerId(memberId);
        Long totalUsageCount = Optional.ofNullable(modelStatisticsRepository.getTotalUsageCountByOwnerId(memberId)).orElse(0L);
        Double averageRating = Optional.ofNullable(reviewRepository.findAverageRatingByOwnerId(memberId, ReviewStatus.ACTIVE)).orElse(0.0);
        Long publicModelCount = aiModelRepository.countPublicModelsByOwnerId(memberId);

        return new UserModelStatsResponse(totalModelCount, totalUsageCount, averageRating, publicModelCount);
    }

    public List<AIModelDetailResponse> getMyModels(Long memberId) {
        return aiModelRepository.findByOwnerId(memberId).stream()
                .map(model -> modelDetailFacadeService.getModelDetail(model.getId(), memberId))
                .toList();
    }
}
