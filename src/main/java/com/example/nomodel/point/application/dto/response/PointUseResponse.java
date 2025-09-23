package com.example.nomodel.point.application.dto.response;

import com.example.nomodel.point.domain.model.PointTransaction;
import com.example.nomodel.point.domain.model.RefererType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PointUseResponse {

    private Long id;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private RefererType refererType;
    private Long refererId;
    private LocalDateTime createdAt;

    public PointUseResponse(PointTransaction transaction) {
        this.id = transaction.getId();
        this.amount = transaction.getPointAmount();
        this.balanceBefore = transaction.getBalanceBefore();
        this.balanceAfter = transaction.getBalanceAfter();
        this.refererType = transaction.getRefererType();
        this.refererId = transaction.getRefererId();
        this.createdAt = transaction.getCreatedAt();
    }
}
