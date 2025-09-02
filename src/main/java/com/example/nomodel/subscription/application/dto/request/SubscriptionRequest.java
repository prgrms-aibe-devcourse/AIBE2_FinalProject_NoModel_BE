package com.example.nomodel.subscription.application.dto.request;

import java.math.BigDecimal;

public class SubscriptionRequest {
    private Long subscriptionId;
    private BigDecimal paidAmount;

    public Long getSubscriptionId() { return subscriptionId; }
    public BigDecimal getPaidAmount() { return paidAmount; }
}
