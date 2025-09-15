package com.example.nomodel.point.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.point.application.dto.request.*;
import com.example.nomodel.point.application.dto.response.*;
import com.example.nomodel.point.application.service.PointPaymentService;
import com.example.nomodel.point.application.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/points")
public class PointController {

    private final PointService pointService;
    private final PointPaymentService pointPaymentService;

    // ✅ 포인트 잔액 조회
    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponse> getBalance(@AuthenticationPrincipal CustomUserDetails user) {
        Long memberId = user.getMemberId();
        return ResponseEntity.ok(pointService.getPointBalance(memberId));
    }

    // ✅ 포인트 거래내역 조회
    @GetMapping("/transactions")
    public ResponseEntity<List<PointTransactionResponse>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = user.getMemberId();
        return ResponseEntity.ok(pointService.getPointTransactions(memberId, page, size));
    }

    // ✅ 포인트 충전 (내부 로직 기반)
    @PostMapping("/charge")
    public ResponseEntity<PointChargeResponse> chargePoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointChargeRequest request
    ) {
        Long memberId = user.getMemberId();
        return ResponseEntity.ok(pointService.chargePoints(memberId, request.getAmount()));
    }

    // ✅ 포인트 사용
    @PostMapping("/use")
    public ResponseEntity<PointUseResponse> usePoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointUseRequest request
    ) {
        Long memberId = user.getMemberId();
        return ResponseEntity.ok(
                pointService.usePoints(memberId, request.getAmount(), request.getRefererId())
        );
    }

    // ✅ 포인트 결제 준비 (카카오페이/토스)
    @PostMapping("/payment/prepare")
    public ResponseEntity<PointPaymentResponse> preparePayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointPaymentRequest request
    ) {
        String merchantUid = pointPaymentService.preparePayment(request.getAmount());
        return ResponseEntity.ok(
                PointPaymentResponse.success(
                        merchantUid,
                        request.getAmount(),
                        request.getPaymentMethod().name()
                )
        );
    }

    // ✅ 결제 검증 및 포인트 충전
    @PostMapping("/payment/verify")
    public ResponseEntity<PointChargeResponse> verifyPaymentAndCharge(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointPaymentVerifyRequest request
    ) {
        Long memberId = user.getMemberId();

        PointPaymentService.PaymentVerificationResult verificationResult =
                pointPaymentService.verifyPayment(request.getImpUid(), request.getMerchantUid(), memberId);

        // 검증 성공 시 포인트 충전
        PointChargeResponse chargeResponse =
                pointService.chargePoints(memberId, verificationResult.getAmount());

        return ResponseEntity.ok(chargeResponse);
    }
}
