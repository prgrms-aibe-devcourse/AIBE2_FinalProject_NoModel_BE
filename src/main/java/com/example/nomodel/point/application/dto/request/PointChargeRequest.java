package com.example.nomodel.point.application.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PointChargeRequest {

    @NotNull(message = "충전 금액은 필수입니다.")
    @Min(value = 1, message = "충전 금액은 1 이상이어야 합니다.")
    private BigDecimal amount;
}
