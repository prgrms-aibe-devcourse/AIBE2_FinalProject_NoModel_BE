package com.example.nomodel.point.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointPaymentVerifyRequest {

    @NotBlank(message = "주문번호는 필수입니다.")
    private String impUid;      // 프론트에서 받아올 imp_uid
    private String merchantUid;  // 필요하면 같이 사용
}