package com.example.nomodel.point.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PointUseRequest {

    @NotNull(message = "사용 금액은 필수입니다.")
    @DecimalMin(value = "1.0", message = "사용 금액은 1 이상이어야 합니다.")
    private BigDecimal amount;

    // 어디서 사용했는지 참조 ID (예: 주문 ID)
    private Long refererId;
}
