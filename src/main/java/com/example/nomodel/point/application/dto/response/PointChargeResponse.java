package com.example.nomodel.point.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.nomodel.point.domain.model.TransactionDirection;
import com.example.nomodel.point.domain.model.TransactionType;
import com.example.nomodel.point.domain.model.RefererType;
import com.example.nomodel.point.domain.model.PointTransaction;
import lombok.Getter;

@Getter
public class PointChargeResponse {

    private Long id;
    private TransactionDirection direction;
    private TransactionType transactionType;
    private BigDecimal pointAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public PointChargeResponse(PointTransaction transaction) {
        this.id = transaction.getId();
        this.direction = transaction.getDirection();
        this.transactionType = transaction.getTransactionType();
        this.pointAmount = transaction.getPointAmount();
        this.balanceBefore = transaction.getBalanceBefore();
        this.balanceAfter = transaction.getBalanceAfter();
        this.createdAt = transaction.getCreatedAt();
    }
}
