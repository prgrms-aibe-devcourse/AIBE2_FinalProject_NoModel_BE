package com.example.nomodel.point.application.dto.response;

import com.example.nomodel.point.domain.model.TransactionDirection;
import com.example.nomodel.point.domain.model.TransactionType;
import com.example.nomodel.point.domain.model.RefererType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PointTransactionResponse {
    private Long id;
    private TransactionDirection direction;
    private TransactionType transactionType;
    private BigDecimal pointAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private RefererType refererType;
    private Long refererId;
    private LocalDateTime createdAt;

    public PointTransactionResponse(Long id, TransactionDirection direction,
                                    TransactionType transactionType,
                                    BigDecimal pointAmount, BigDecimal balanceBefore,
                                    BigDecimal balanceAfter, RefererType refererType,
                                    Long refererId, LocalDateTime createdAt) {
        this.id = id;
        this.direction = direction;
        this.transactionType = transactionType;
        this.pointAmount = pointAmount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.refererType = refererType;
        this.refererId = refererId;
        this.createdAt = createdAt;
    }

    // getter...
}
