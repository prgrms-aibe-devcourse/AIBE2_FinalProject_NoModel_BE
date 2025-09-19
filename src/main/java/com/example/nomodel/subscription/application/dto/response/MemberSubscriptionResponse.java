package com.example.nomodel.subscription.application.dto.response;

import com.example.nomodel.subscription.domain.model.MemberSubscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSubscriptionResponse {
    private Long id;
    private Long memberId;
    private Long subscriptionId;
    private String status;
    private Boolean autoRenewal;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private BigDecimal paidAmount;

    // Entity -> DTO 변환
    public static MemberSubscriptionResponse from(MemberSubscription entity) {
        return MemberSubscriptionResponse.builder()
                .id(entity.getId())
                .memberId(entity.getMemberId())
                .subscriptionId(entity.getSubscription().getId())
                .status(entity.getStatus().name()) // Enum이면 .name() or getValue()
                .autoRenewal(entity.getAutoRenewal())
                .startedAt(entity.getStartedAt())
                .expiresAt(entity.getExpiresAt())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason() != null
                        ? entity.getCancellationReason().name()
                        : null)
                .paidAmount(entity.getPaidAmount())
                .build();
    }

    // 미구독 상태를 나타내는 empty 응답
    public static MemberSubscriptionResponse empty() {
        return MemberSubscriptionResponse.builder()
                .id(null)
                .memberId(null)
                .subscriptionId(null)
                .status("NONE") // 또는 "UNSUBSCRIBED" 등 미구독 상태를 나타내는 값
                .autoRenewal(false)
                .startedAt(null)
                .expiresAt(null)
                .cancelledAt(null)
                .cancellationReason(null)
                .paidAmount(BigDecimal.ZERO)
                .build();
    }
}
