package com.example.nomodel.point.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointChargeRequest {

    @NotNull(message = "충전 금액은 필수입니다.")
    @DecimalMin(value = "1.0", inclusive = true, message = "충전 금액은 1 이상이어야 합니다.")
    private BigDecimal amount;

}
