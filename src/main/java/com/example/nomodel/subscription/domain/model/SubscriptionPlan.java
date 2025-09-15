package com.example.nomodel.subscription.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PlanType planType;  // BASIC, STANDARD, PREMIUM, ENTERPRISE

    private String name;        // 플랜 이름 (ex. Basic Plan)
    private String description; // 상세 설명
    private Integer periodDays; // 기간 (30, 365일 등)
    private Integer dailyLimit; // 일일 사용 제한 (-1 = 무제한)
    private BigDecimal price;   // 원화 기준 가격 (예: 13500)

    protected SubscriptionPlan() {
    }
}
