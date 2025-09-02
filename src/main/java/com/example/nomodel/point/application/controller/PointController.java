package com.example.nomodel.point.application.controller;

import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.application.service.PointService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members/me/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping
    public PointBalanceResponse getBalance(@RequestHeader("X-Member-Id") Long memberId) {
        return pointService.getBalance(memberId);
    }

    @PostMapping("/charge")
    public PointTransactionResponse addPoints(@RequestHeader("X-Member-Id") Long memberId,
                                              @RequestBody PointUseRequest request) {
        return pointService.addPoints(memberId, request);
    }

    @PostMapping("/use")
    public PointTransactionResponse usePoints(@RequestHeader("X-Member-Id") Long memberId,
                                              @RequestBody PointUseRequest request) {
        return pointService.usePoints(memberId, request);
    }

    @GetMapping("/transactions")
    public List<PointTransactionResponse> getTransactions(@RequestHeader("X-Member-Id") Long memberId) {
        return pointService.getTransactions(memberId);
    }
}
