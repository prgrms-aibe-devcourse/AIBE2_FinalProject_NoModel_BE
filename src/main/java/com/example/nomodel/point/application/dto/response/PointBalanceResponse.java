package com.example.nomodel.point.application.dto.response;

import java.math.BigDecimal;

public class PointBalanceResponse {
    private BigDecimal totalPoints;
    private BigDecimal availablePoints;
    private BigDecimal pendingPoints;
    private BigDecimal reservedPoints;

    public PointBalanceResponse(BigDecimal totalPoints, BigDecimal availablePoints,
                                BigDecimal pendingPoints, BigDecimal reservedPoints) {
        this.totalPoints = totalPoints;
        this.availablePoints = availablePoints;
        this.pendingPoints = pendingPoints;
        this.reservedPoints = reservedPoints;
    }

    // getter
    public BigDecimal getTotalPoints() { return totalPoints; }
    public BigDecimal getAvailablePoints() { return availablePoints; }
    public BigDecimal getPendingPoints() { return pendingPoints; }
    public BigDecimal getReservedPoints() { return reservedPoints; }
}
