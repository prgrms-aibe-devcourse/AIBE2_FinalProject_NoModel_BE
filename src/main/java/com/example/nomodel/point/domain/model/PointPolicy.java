package com.example.nomodel.point.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_policy")
public class PointPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String policyType; // EARN, SPEND
    private String referType;  // REVIEW, ORDER, STORE

    private BigDecimal pointAmount;

    private Boolean isActive;
    private Integer priority;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
