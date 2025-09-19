package com.example.nomodel.point.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
public class PointPaymentRequest {
    private String amount;
    private PaymentMethod paymentMethod;

    public BigDecimal getAmountAsBigDecimal() {
        return new BigDecimal(amount);
    }

    public enum PaymentMethod {
        KAKAO
    }
}

