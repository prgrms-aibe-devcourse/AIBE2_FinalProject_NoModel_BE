package com.example.nomodel.subscription.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    private Long period; // 일 단위
    private Integer dailyLimit;
    private Integer selfMadeModelNum;

    protected Subscription() {}

    public Subscription(String name, String description, BigDecimal price, Long period) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.period = period;
        this.dailyLimit = 0;
        this.selfMadeModelNum = 0;
    }

    // getter
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Long getPeriod() { return period; }
    public Integer getDailyLimit() { return dailyLimit; }
    public Integer getSelfMadeModelNum() { return selfMadeModelNum; }
}
