package com.example.nomodel.coupon.application.controller;

import com.example.nomodel.coupon.application.dto.request.CouponCreateRequest;
import com.example.nomodel.coupon.application.dto.response.CouponResponse;
import com.example.nomodel.coupon.application.service.CouponService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // 관리자: 쿠폰 등록
    @PostMapping("/admin")
    public CouponResponse createCoupon(@RequestBody CouponCreateRequest request) {
        return couponService.createCoupon(request);
    }

    // 사용자: 보유 쿠폰 조회
    @GetMapping("/members/{memberId}")
    public List<CouponResponse> getCoupons(@PathVariable Long memberId) {
        return couponService.getCouponsByMember(memberId);
    }

    // 사용자: 쿠폰 적용
    @PostMapping("/{couponCode}/apply")
    public CouponResponse applyCoupon(@PathVariable String couponCode,
                                      @RequestHeader("X-Member-Id") Long memberId) {
        return couponService.applyCoupon(couponCode, memberId);
    }
}
