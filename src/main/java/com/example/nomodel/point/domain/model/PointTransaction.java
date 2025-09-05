package com.example.nomodel.point.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_transaction")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private BigDecimal pointAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    private RefererType refererType;
    private Long refererId;

    private LocalDateTime createdAt;

    protected PointTransaction() {}

    public PointTransaction(Long memberId,
                            TransactionDirection direction,
                            TransactionType transactionType,
                            BigDecimal pointAmount,
                            BigDecimal balanceBefore,
                            BigDecimal balanceAfter,
                            RefererType refererType,
                            Long refererId) {
        this.memberId = memberId;
        this.direction = direction;
        this.transactionType = transactionType;
        this.pointAmount = pointAmount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.refererType = refererType;
        this.refererId = refererId;
        this.createdAt = LocalDateTime.now();
    }

    // getter
    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public TransactionDirection getDirection() { return direction; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getPointAmount() { return pointAmount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public RefererType getRefererType() { return refererType; }
    public Long getRefererId() { return refererId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
