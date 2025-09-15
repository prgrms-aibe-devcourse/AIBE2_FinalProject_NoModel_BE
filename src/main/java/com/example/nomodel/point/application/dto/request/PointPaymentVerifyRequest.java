package com.example.nomodel.point.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointPaymentVerifyRequest {

    @NotBlank(message = "주문번호는 필수입니다.")
    private String merchantUid;
}