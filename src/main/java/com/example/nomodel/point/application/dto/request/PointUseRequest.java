package com.example.nomodel.point.application.dto.request;

import com.example.nomodel.point.domain.model.TransactionType;
import com.example.nomodel.point.domain.model.RefererType;

import java.math.BigDecimal;

public class PointUseRequest {
    private BigDecimal amount;
    private TransactionType transactionType;
    private RefererType refererType;
    private Long refererId;

    public BigDecimal getAmount() { return amount; }
    public TransactionType getTransactionType() { return transactionType; }
    public RefererType getRefererType() { return refererType; }
    public Long getRefererId() { return refererId; }
}
