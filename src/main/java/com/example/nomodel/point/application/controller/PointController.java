package com.example.nomodel.point.application.controller;

import com.example.nomodel._core.security.CustomUserDetails;
import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.point.application.dto.request.PointChargeRequest;
import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointChargeResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.application.dto.response.PointUseResponse;
import com.example.nomodel.point.application.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/points") // memberId 제거
public class PointController {

    private final PointService pointService;

    // 포인트 잔액 조회
    @GetMapping("/balance")
    public ApiUtils.ApiResult<?> getBalance(@AuthenticationPrincipal CustomUserDetails user) {
        Long memberId = user.getMemberId(); // JWT에서 추출
        PointBalanceResponse response = pointService.getPointBalance(memberId);
        return ApiUtils.success(response);
    }

    // 포인트 거래내역 조회
    @GetMapping("/transactions")
    public ApiUtils.ApiResult<?> getTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = user.getMemberId();
        List<PointTransactionResponse> response = pointService.getPointTransactions(memberId, page, size);
        return ApiUtils.success(response);
    }

    // 포인트 충전
    @PostMapping("/charge")
    public ApiUtils.ApiResult<?> chargePoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointChargeRequest request
    ) {
        Long memberId = user.getMemberId();
        PointChargeResponse response = pointService.chargePoints(memberId, request.getAmount());
        return ApiUtils.success(response);
    }

    @PostMapping("/use")
    public ApiUtils.ApiResult<?> usePoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid PointUseRequest request
    ) {
        try {
            Long memberId = user.getMemberId();
            PointUseResponse response = pointService.usePoints(memberId, request.getAmount(), request.getRefererId());
            return ApiUtils.success(response);
        } catch (Exception e) {
            return ApiUtils.error("포인트 사용에 실패했습니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
