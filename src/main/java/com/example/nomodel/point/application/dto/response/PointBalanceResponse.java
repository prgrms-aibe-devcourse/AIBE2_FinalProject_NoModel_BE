package com.example.nomodel.point.application.dto.response;

import com.example.nomodel.point.domain.model.MemberPointBalance;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PointBalanceResponse {
    private Long memberId;
    private BigDecimal totalPoints;
    private BigDecimal availablePoints;
    private BigDecimal pendingPoints;
    private BigDecimal reservedPoints;

    public static PointBalanceResponse from(MemberPointBalance balance) {
        return new PointBalanceResponse(
                balance.getMemberId(),
                balance.getTotalPoints(),
                balance.getAvailablePoints(),
                balance.getPendingPoints(),
                balance.getReservedPoints()
        );
    }
}
