package com.example.nomodel.subscription.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class SubscriptionRequest {
    private Long subscriptionId;
    private BigDecimal paidAmount;
    private Long paymentMethodId;
    private String customerUid;

    public SubscriptionRequest(Long subscriptionId, Long paymentMethodId, Long couponId, BigDecimal paidAmount) {
        this.subscriptionId = subscriptionId;
        this.paymentMethodId = paymentMethodId;
        this.paidAmount = paidAmount;
        this.customerUid = customerUid;
    }
}
