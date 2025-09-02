package com.example.nomodel.point.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "member_point_balance")
public class MemberPointBalance {

    @Id
    private Long memberId;

    private BigDecimal totalPoints;
    private BigDecimal availablePoints;
    private BigDecimal pendingPoints;
    private BigDecimal reservedPoints;

    @Version
    private Long version;

    protected MemberPointBalance() {}

    public MemberPointBalance(Long memberId) {
        this.memberId = memberId;
        this.totalPoints = BigDecimal.ZERO;
        this.availablePoints = BigDecimal.ZERO;
        this.pendingPoints = BigDecimal.ZERO;
        this.reservedPoints = BigDecimal.ZERO;
    }

    // getter
    public Long getMemberId() { return memberId; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public BigDecimal getAvailablePoints() { return availablePoints; }
    public BigDecimal getPendingPoints() { return pendingPoints; }
    public BigDecimal getReservedPoints() { return reservedPoints; }

    // 포인트 추가/차감 로직
    public void addPoints(BigDecimal amount) {
        this.totalPoints = this.totalPoints.add(amount);
        this.availablePoints = this.availablePoints.add(amount);
    }

    public void subtractPoints(BigDecimal amount) {
        if (this.availablePoints.compareTo(amount) < 0) {
            throw new IllegalArgumentException("보유 포인트가 부족합니다.");
        }
        this.totalPoints = this.totalPoints.subtract(amount);
        this.availablePoints = this.availablePoints.subtract(amount);
    }
}
