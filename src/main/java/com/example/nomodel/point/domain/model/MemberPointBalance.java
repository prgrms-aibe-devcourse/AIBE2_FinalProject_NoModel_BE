package com.example.nomodel.point.domain.model;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
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

    public MemberPointBalance(Long memberId, BigDecimal availablePoints) {
        this.memberId = memberId;

        if (availablePoints == null) {
            throw new ApplicationException(ErrorCode.POINT_INVALID_INIT);
        }
        if (availablePoints.signum() < 0) {
            throw new ApplicationException(ErrorCode.POINT_INVALID_INIT);
        }

        this.totalPoints = availablePoints;
        this.availablePoints = availablePoints;
        this.pendingPoints = BigDecimal.ZERO;
        this.reservedPoints = BigDecimal.ZERO;
    }

    public MemberPointBalance(Long memberId) {
        this(memberId, BigDecimal.ZERO);
    }

    // ===== Getter =====
    public Long getMemberId() { return memberId; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public BigDecimal getAvailablePoints() { return availablePoints; }
    public BigDecimal getPendingPoints() { return pendingPoints; }
    public BigDecimal getReservedPoints() { return reservedPoints; }

    // ===== 기본 포인트 입출금 =====
    public void addPoints(BigDecimal amount) {
        validateAmount(amount);
        this.totalPoints = this.totalPoints.add(amount);
        this.availablePoints = this.availablePoints.add(amount);
    }

    public void subtractPoints(BigDecimal amount) {
        validateAmount(amount);
        if (this.availablePoints.compareTo(amount) < 0) {
            throw new ApplicationException(ErrorCode.POINT_INSUFFICIENT_BALANCE);
        }
        this.totalPoints = this.totalPoints.subtract(amount);
        this.availablePoints = this.availablePoints.subtract(amount);
    }

    // ===== 보류 포인트 (pending) 관리 =====
    public void addPendingPoints(BigDecimal amount) {
        validateAmount(amount);
        this.totalPoints = this.totalPoints.add(amount);
        this.pendingPoints = this.pendingPoints.add(amount);
    }

    public void confirmPendingToAvailable(BigDecimal amount) {
        validateAmount(amount);
        if (this.pendingPoints.compareTo(amount) < 0) {
            throw new ApplicationException(ErrorCode.POINT_INVALID_AMOUNT);
        }
        this.pendingPoints = this.pendingPoints.subtract(amount);
        this.availablePoints = this.availablePoints.add(amount);
    }

    // ===== 예약 포인트 (reserved) 관리 =====
    public void reservePoints(BigDecimal amount) {
        validateAmount(amount);
        if (this.availablePoints.compareTo(amount) < 0) {
            throw new ApplicationException(ErrorCode.POINT_INSUFFICIENT_BALANCE);
        }
        this.availablePoints = this.availablePoints.subtract(amount);
        this.reservedPoints = this.reservedPoints.add(amount);
    }

    public void releaseReservedPoints(BigDecimal amount) {
        validateAmount(amount);
        if (this.reservedPoints.compareTo(amount) < 0) {
            throw new ApplicationException(ErrorCode.POINT_INVALID_AMOUNT);
        }
        this.reservedPoints = this.reservedPoints.subtract(amount);
        this.availablePoints = this.availablePoints.add(amount);
    }

    // ===== 유효성 검사 =====
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new ApplicationException(ErrorCode.POINT_INVALID_AMOUNT);
        }
    }
}
