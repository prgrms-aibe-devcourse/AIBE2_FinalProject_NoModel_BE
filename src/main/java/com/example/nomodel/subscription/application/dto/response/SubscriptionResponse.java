package com.example.nomodel.subscription.application.dto.response;

import java.math.BigDecimal;

public class SubscriptionResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long period;

    public SubscriptionResponse(Long id, String name, String description, BigDecimal price, Long period) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.period = period;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Long getPeriod() { return period; }
}
