package com.example.nomodel.subscription.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public MemberSubscriptionResponse(Long id, Long memberId, Long subscriptionId, String status,
                                      Boolean autoRenewal, LocalDateTime startedAt, LocalDateTime expiresAt,
                                      LocalDateTime cancelledAt, String cancellationReason, BigDecimal paidAmount) {
        this.id = id;
        this.memberId = memberId;
        this.subscriptionId = subscriptionId;
        this.status = status;
        this.autoRenewal = autoRenewal;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
        this.paidAmount = paidAmount;
    }

    // getter...
}
