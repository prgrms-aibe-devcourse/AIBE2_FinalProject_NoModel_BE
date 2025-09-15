package com.example.nomodel.point.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PointPaymentRequest {

    @NotNull(message = "충전 금액은 필수입니다.")
    @DecimalMin(value = "1000.0", inclusive = true, message = "최소 충전 금액은 1,000원입니다.")
    private BigDecimal amount;

    @NotNull(message = "결제 수단은 필수입니다.")
    private PaymentMethod paymentMethod;

    public enum PaymentMethod {
        KAKAO, TOSS
    }
}