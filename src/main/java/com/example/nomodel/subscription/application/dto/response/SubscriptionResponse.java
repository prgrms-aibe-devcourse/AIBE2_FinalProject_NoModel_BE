package com.example.nomodel.subscription.application.dto.response;

import com.example.nomodel.subscription.domain.model.PlanType;
import java.math.BigDecimal;

public class SubscriptionResponse {
    private Long id;
    private PlanType planType;
    private String description;
    private BigDecimal price;
    private Integer period; // 일 단위니까 Long이 더 자연스러움

    public SubscriptionResponse(Long id, PlanType planType, String description, BigDecimal price, Integer period) {
        this.id = id;
        this.planType = planType;
        this.description = description;
        this.price = price;
        this.period = period;
    }

    public Long getId() { return id; }
    public PlanType getPlanType() { return planType; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getPeriod() { return period; }
}
