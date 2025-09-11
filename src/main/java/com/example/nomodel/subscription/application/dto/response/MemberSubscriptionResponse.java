package com.example.nomodel.subscription.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.security.DenyAll;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
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

}
