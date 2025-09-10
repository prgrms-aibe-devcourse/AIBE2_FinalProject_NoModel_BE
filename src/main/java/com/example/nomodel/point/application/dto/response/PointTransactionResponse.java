package com.example.nomodel.point.application.dto.response;

import com.example.nomodel.point.domain.model.PointTransaction;
import com.example.nomodel.point.domain.model.TransactionDirection;
import com.example.nomodel.point.domain.model.TransactionType;
import com.example.nomodel.point.domain.model.RefererType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
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

    public static PointTransactionResponse from(PointTransaction transaction) {
        return new PointTransactionResponse(
                transaction.getId(),
                transaction.getDirection(),
                transaction.getTransactionType(),   // String 반환
                transaction.getPointAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getRefererType(),       // String 반환
                transaction.getRefererId(),
                transaction.getCreatedAt()
        );
    }
}
