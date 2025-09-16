package com.example.nomodel.point.application.dto.response;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PointPaymentResponse {

    private final String merchantUid;
    private final BigDecimal amount;
    private final String paymentMethod;
    private final String message;

    public PointPaymentResponse(String merchantUid, BigDecimal amount, String paymentMethod) {
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.message = "결제가 준비되었습니다. 결제를 진행해주세요.";
    }

    public static PointPaymentResponse success(String merchantUid, BigDecimal amount, String paymentMethod) {
        return new PointPaymentResponse(merchantUid, amount, paymentMethod);
    }
}