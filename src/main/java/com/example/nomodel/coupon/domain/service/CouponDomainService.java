package com.example.nomodel.coupon.domain.service;

import com.example.nomodel.coupon.domain.model.Coupon;
import com.example.nomodel.coupon.domain.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponDomainService {

    private final CouponRepository couponRepository;

    public CouponDomainService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public List<Coupon> getCouponsByMember(Long memberId) {
        return couponRepository.findAll().stream()
                .filter(c -> memberId.equals(c.getUsedBy()) || c.getUsedBy() == null)
                .toList();
    }

    public Coupon applyCoupon(String couponCode, Long memberId) {
        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));
        if (!coupon.isValid()) {
            throw new IllegalStateException("쿠폰이 유효하지 않습니다.");
        }
        coupon.use(memberId);
        return couponRepository.save(coupon);
    }
}
