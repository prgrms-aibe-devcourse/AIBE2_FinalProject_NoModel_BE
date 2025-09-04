package com.example.nomodel.point.application.service;

import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.domain.model.*;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.point.domain.service.PointDomainService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointDomainService domainService;
    private final MemberPointBalanceRepository pointBalanceRepository;
    private final PointTransactionRepository transactionRepository;

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


    @Transactional
        public void rewardForReview(Long memberId, Long reviewId) {
            MemberPointBalance balance = pointBalanceRepository.findById(memberId)
                    .orElse(new MemberPointBalance(memberId, BigDecimal.ZERO));

            BigDecimal before = balance.getAvailablePoints();
            BigDecimal rewardAmount = BigDecimal.valueOf(300);

            balance.addPoints(rewardAmount);
            pointBalanceRepository.save(balance);

            PointTransaction tx = new PointTransaction(
                    memberId,
                    TransactionDirection.CREDIT,
                    TransactionType.REWARD,
                    rewardAmount,
                    before,
                    balance.getAvailablePoints(),
                    RefererType.REVIEW,
                    reviewId
            );
            transactionRepository.save(tx);
        }
    }

