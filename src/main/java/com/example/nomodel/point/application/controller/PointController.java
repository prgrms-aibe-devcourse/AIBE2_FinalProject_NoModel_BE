package com.example.nomodel.point.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel.point.application.dto.request.PointChargeRequest;
import com.example.nomodel.point.application.dto.request.PointPaymentRequest;
import com.example.nomodel.point.application.dto.request.PointPaymentVerifyRequest;
import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointChargeResponse;
import com.example.nomodel.point.application.dto.response.PointPaymentResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.application.dto.response.PointUseResponse;
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
@RequestMapping("/points") // memberId 제거
public class PointController {

    private final PointService pointService;
    private final PointPaymentService pointPaymentService;

    // 포인트 잔액 조회
    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponse> getBalance(@AuthenticationPrincipal CustomUserDetails user) {
        Long memberId = user.getMemberId(); // JWT에서 추출
        PointBalanceResponse response = pointService.getPointBalance(memberId);
        return ResponseEntity.ok(response);
    }

    // 포인트 거래내역 조회
    @GetMapping("/transactions")
    public ResponseEntity<List<PointTransactionResponse>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = user.getMemberId();
        List<PointTransactionResponse> response = pointService.getPointTransactions(memberId, page, size);
        return ResponseEntity.ok(response);
    }

    // 포인트 충전
    @PostMapping("/charge")
    public ResponseEntity<PointChargeResponse> chargePoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointChargeRequest request
    ) {
        Long memberId = user.getMemberId();
        PointChargeResponse response = pointService.chargePoints(memberId, request.getAmount());
        return ResponseEntity.ok(response);
    }

    // 포인트 사용
    @PostMapping("/use")
    public ResponseEntity<PointUseResponse> usePoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointUseRequest request
    ) {
        Long memberId = user.getMemberId();
        PointUseResponse response = pointService.usePoints(memberId, request.getAmount(), request.getRefererId());
        return ResponseEntity.ok(response);
    }

    // 포인트 결제 준비 (카카오페이/토스페이)
    @PostMapping("/payment/prepare")
    public ResponseEntity<PointPaymentResponse> preparePayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointPaymentRequest request
    ) {
        Long memberId = user.getMemberId();
        String merchantUid = null;

        switch (request.getPaymentMethod()) {
            case KAKAO:
                merchantUid = pointPaymentService.processKakaoPointCharge(memberId, request.getAmount());
                break;
            case TOSS:
                merchantUid = pointPaymentService.processTossPointCharge(memberId, request.getAmount());
                break;
        }

        if (merchantUid != null) {
            PointPaymentResponse response = PointPaymentResponse.success(
                    merchantUid,
                    request.getAmount(),
                    request.getPaymentMethod().name()
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    // 결제 검증 및 포인트 충전
    @PostMapping("/payment/verify")
    public ResponseEntity<PointChargeResponse> verifyPaymentAndCharge(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointPaymentVerifyRequest request
    ) {
        Long memberId = user.getMemberId();

        // 1. 결제 검증
        PointPaymentService.PaymentVerificationResult verificationResult =
                pointPaymentService.verifyPayment(request.getMerchantUid());

        if (!verificationResult.isSuccess()) {
            return ResponseEntity.badRequest().build();
        }

        // 2. 포인트 충전 (결제 참조번호와 함께)
        PointChargeResponse chargeResponse = pointService.chargePointsWithReference(
                memberId,
                verificationResult.getAmount(),
                verificationResult.getMerchantUid()
        );

        return ResponseEntity.ok(chargeResponse);
    }
}
