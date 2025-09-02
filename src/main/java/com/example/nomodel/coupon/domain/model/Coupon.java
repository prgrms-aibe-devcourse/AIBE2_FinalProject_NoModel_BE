package com.example.nomodel.coupon.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String couponCode;
    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal discountValue;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private LocalDateTime usedAt;
    private Long usedBy;

    protected Coupon() {}

    public Coupon(String couponCode, String name, DiscountType discountType,
                  BigDecimal discountValue, LocalDateTime validFrom, LocalDateTime validUntil) {
        this.couponCode = couponCode;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.status = CouponStatus.ACTIVE;
    }

    // getter
    public Long getId() { return id; }
    public String getCouponCode() { return couponCode; }
    public String getName() { return name; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public CouponStatus getStatus() { return status; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public Long getUsedBy() { return usedBy; }

    // 비즈니스 메서드
    public void use(Long memberId) {
        this.status = CouponStatus.USED;
        this.usedBy = memberId;
        this.usedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return status == CouponStatus.ACTIVE &&
                now.isAfter(validFrom) && now.isBefore(validUntil);
    }
}
