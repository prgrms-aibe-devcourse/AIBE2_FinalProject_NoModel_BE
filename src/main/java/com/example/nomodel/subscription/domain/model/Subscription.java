package com.example.nomodel.subscription.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType;
    private String description;
    private BigDecimal price;
    private Long period; // 일 단위
    private Integer dailyLimit;
    private Integer selfMadeModelNum;

    protected Subscription() {}

    public Subscription(PlanType planType, String description, BigDecimal price, Long period) {
        this.planType = planType;
        this.description = description;
        this.period = period;
        this.dailyLimit = dailyLimit;
        this.price = price;
        this.selfMadeModelNum = selfMadeModelNum;
    }

    // getter
    public Long getId() { return id; }
    public PlanType getPlanType() { return planType; }
    public String getDescription() { return description; }
    public Long getPeriod() { return period; }
    public Integer getDailyLimit() { return dailyLimit; }
    public BigDecimal getPrice() { return price; }
    public Integer getSelfMadeModelNum() { return selfMadeModelNum; }
}