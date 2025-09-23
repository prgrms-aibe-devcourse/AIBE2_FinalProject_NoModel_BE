package com.example.nomodel.point.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointChargeResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
import com.example.nomodel.point.application.dto.response.PointUseResponse;
import com.example.nomodel.point.domain.model.*;
import com.example.nomodel.point.domain.policy.PointRewardPolicy;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.point.domain.service.PointDomainService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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
    private final PointRewardPolicy rewardPolicy;


    @Transactional(readOnly = true)
    public PointBalanceResponse getPointBalance(Long memberId) {
        MemberPointBalance balance = pointBalanceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));

        return PointBalanceResponse.from(balance);
        }
    /**
     * 리뷰 보상 포인트 지급
     * 정책: 한 모델당 한 번만 보상
     */
    @Transactional
    public void rewardForReview(Long reviewerId, Long modelId) {
        BigDecimal rewardAmount = rewardPolicy.getReviewRewardAmount();

        // 1. 정책 위반 체크: 같은 모델에 대해 이미 보상받았는지 확인 (빠른 실패)
        boolean alreadyRewarded = transactionRepository
                .existsByMemberIdAndRefererTypeAndRefererIdAndTransactionType(
                        reviewerId,
                        RefererType.REVIEW,
                        modelId,
                        TransactionType.REWARD
                );

        if (alreadyRewarded) {
            throw new ApplicationException(ErrorCode.DUPLICATE_REVIEW_REWARD);
        }

        //2. 회원 포인트 잔액 조회 또는 생성
        MemberPointBalance balance = pointBalanceRepository.findByMemberId(reviewerId)
                .orElseGet(() -> {
                    // 잔액이 없으면 새로 생성
                    MemberPointBalance newBalance = new MemberPointBalance(reviewerId);
                    return pointBalanceRepository.save(newBalance);
                });

        // 3. 거래 내역 생성
        BigDecimal before = balance.getTotalPoints();
        BigDecimal after = before.add(rewardAmount);

        PointTransaction transaction = new PointTransaction(
                reviewerId,
                TransactionDirection.CREDIT,
                TransactionType.REWARD,
                rewardAmount,
                before,
                after,
                RefererType.REVIEW,
                modelId
        );

        // 4. 거래 내역 저장 (중복 보상 DB 제약 예외 처리)
        try {
            transactionRepository.save(transaction);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ApplicationException(ErrorCode.DUPLICATE_REVIEW_REWARD);
        }

        // 5. 보류 포인트 적립
        balance.addPendingPoints(rewardAmount);
        pointBalanceRepository.save(balance);

        // 6. 5초 뒤 보류 → 가용 포인트 전환 (시연용)
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 5초 대기
                MemberPointBalance latest = pointBalanceRepository.findByMemberId(reviewerId)
                        .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));
                latest.confirmPendingToAvailable(rewardAmount);
                pointBalanceRepository.save(latest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Transactional(readOnly = true)
    public List<PointTransactionResponse> getPointTransactions(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PointTransaction> transactions = transactionRepository.findByMemberId(memberId, pageable);

        return transactions.stream()
                .map(PointTransactionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PointChargeResponse chargePoints(Long memberId, BigDecimal amount) {
        // paymentReference가 없는 경우 (예: 관리자 직접 충전)
        return chargePointsWithReference(memberId, amount, null);
    }

    @Transactional
    public PointChargeResponse chargePointsWithReference(Long memberId, BigDecimal amount, String paymentReference) {
        // PointDomainService의 chargePoints를 호출하고 동기적으로 블로킹
        domainService.chargePoints(memberId, amount, paymentReference).block();

        // TODO: 충전 결과에 따라 PointChargeResponse를 생성하여 반환 (현재는 단순화)
        // 실제로는 PointDomainService에서 반환하는 PointTransaction 정보를 사용하여 DTO를 생성해야 함
        return new PointChargeResponse(null); // 임시 반환 값
    }

    @Transactional
    public PointUseResponse usePoints(Long memberId, BigDecimal amount, Long refererId) {
        // 유효성 검사
        if (amount == null || amount.signum() <= 0) {
            throw new ApplicationException(ErrorCode.POINT_INVALID_AMOUNT);
        }

        // 1. 회원 포인트 잔액 조회
        MemberPointBalance balance = pointBalanceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 잔액 차감
        balance.subtractPoints(amount);
        pointBalanceRepository.save(balance);

        // 3. 거래 내역 생성
        PointTransaction transaction = new PointTransaction(
                memberId,
                TransactionDirection.DEBIT,   // 차감
                TransactionType.USE,         // 사용
                amount,
                balance.getTotalPoints().add(amount), // 차감 전 잔액
                balance.getTotalPoints(),             // 차감 후 잔액
                RefererType.ORDER,                    // 사용은 보통 주문과 연관
                refererId
        );
        transactionRepository.save(transaction);

        return new PointUseResponse(transaction);
    }





}



