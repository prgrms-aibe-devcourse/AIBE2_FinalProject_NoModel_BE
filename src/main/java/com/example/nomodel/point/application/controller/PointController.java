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
import java.util.Map;

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



    // ✅ 결제 검증 및 포인트 충전
    @PostMapping("/payment/verify")
    public ResponseEntity<Map<String, Object>> verifyPaymentAndCharge(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointPaymentVerifyRequest request
    ) {
        Long memberId = user.getMemberId();

        try {
            PointPaymentService.PaymentVerificationResult verificationResult =
                    pointPaymentService.verifyPayment(request.getImpUid(), request.getMerchantUid(), memberId);

            PointBalanceResponse balanceResponse = pointService.getPointBalance(memberId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "amount", verificationResult.getAmount(),
                    "availablePoints", balanceResponse.getAvailablePoints(),
                    "message", "포인트 충전이 완료되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "결제 검증 실패: " + e.getMessage()
            ));
        }
    }
}

