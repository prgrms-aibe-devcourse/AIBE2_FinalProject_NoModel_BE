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
    private PlanType planType;   // BASIC, STANDARD, PREMIUM, ENTERPRISE

    private String name;         // 예: Basic Plan
    private String description;  // 플랜 설명
    private Integer period;      // 구독 기간 (일 단위, 예: 30일)
    private Integer dailyLimit;  // 하루 사용 제한 (-1 = 무제한)
    private BigDecimal price;    // 가격 (KRW 기준)

    private Integer selfMadeModelNum; // 자체 제작 모델 허용 개수 (ERD에 있던 필드)

    protected Subscription() {}

    public Subscription(PlanType planType, String name, String description,
                        Integer period, Integer dailyLimit, BigDecimal price,
                        Integer selfMadeModelNum) {
        this.planType = planType;
        this.name = name;
        this.description = description;
        this.period = period;
        this.dailyLimit = dailyLimit;
        this.price = price;
        this.selfMadeModelNum = selfMadeModelNum;
    }

    // getter
    public Long getId() { return id; }
    public PlanType getPlanType() { return planType; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getPeriod() { return period; }
    public Integer getDailyLimit() { return dailyLimit; }
    public BigDecimal getPrice() { return price; }
    public Integer getSelfMadeModelNum() { return selfMadeModelNum; }
}