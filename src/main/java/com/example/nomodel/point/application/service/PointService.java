package com.example.nomodel.point.application.service;

import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.domain.model.MemberPointBalance;
import com.example.nomodel.point.domain.model.PointTransaction;
import com.example.nomodel.point.domain.service.PointDomainService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PointService {

    private final PointDomainService domainService;

    public PointService(PointDomainService domainService) {
        this.domainService = domainService;
    }

    public PointBalanceResponse getBalance(Long memberId) {
        MemberPointBalance balance = domainService.getBalance(memberId);
        return new PointBalanceResponse(
                balance.getTotalPoints(),
                balance.getAvailablePoints(),
                balance.getPendingPoints(),
                balance.getReservedPoints()
        );
    }

    public PointTransactionResponse addPoints(Long memberId, PointUseRequest request) {
        PointTransaction tx = domainService.addPoints(
                memberId, request.getAmount(),
                request.getTransactionType(),
                request.getRefererType(),
                request.getRefererId()
        );
        return toResponse(tx);
    }

    public PointTransactionResponse usePoints(Long memberId, PointUseRequest request) {
        PointTransaction tx = domainService.usePoints(
                memberId, request.getAmount(),
                request.getTransactionType(),
                request.getRefererType(),
                request.getRefererId()
        );
        return toResponse(tx);
    }

    public List<PointTransactionResponse> getTransactions(Long memberId) {
        return domainService.getTransactions(memberId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PointTransactionResponse toResponse(PointTransaction tx) {
        return new PointTransactionResponse(
                tx.getId(),
                tx.getDirection(),
                tx.getTransactionType(),
                tx.getPointAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getRefererType(),
                tx.getRefererId(),
                tx.getCreatedAt()
        );
    }
}
