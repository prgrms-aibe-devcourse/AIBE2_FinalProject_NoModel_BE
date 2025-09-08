package com.example.nomodel.point.application.controller;

import com.example.nomodel._core.utils.ApiUtils;
import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.application.service.PointService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members/{memberId}/points")
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public ApiUtils.ApiResult<?> getBalance(@PathVariable Long memberId) {
        try {
            PointBalanceResponse response = pointService.getPointBalance(memberId);
            return ApiUtils.success(response);
        } catch (Exception e) {
            return ApiUtils.error("해당 회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }

    // 포인트 거래내역 조회
    @GetMapping("/transactions")
    public ApiUtils.ApiResult<?> getTransactions(@PathVariable Long memberId) {
        try {
            List<PointTransactionResponse> response = pointService.getPointTransactions(memberId, 0, 10);

            return ApiUtils.success(response);
        } catch (Exception e) {
            return ApiUtils.error("거래내역을 조회할 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }
}
