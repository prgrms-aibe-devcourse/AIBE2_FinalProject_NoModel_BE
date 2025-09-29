package com.example.nomodel.point.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PointUseRequest {

    @NotNull(message = "사용 금액은 필수입니다.")
    @DecimalMin(value = "1.0", message = "사용 금액은 1 이상이어야 합니다.")
    private BigDecimal amount;

    // 어디서 사용했는지 참조 ID (예: 주문 ID)
    @NotNull(message = "참조 ID는 필수입니다.")
    @Positive(message = "참조 ID는 양수여야 합니다.")
    private Long refererId;
}
