package com.example.nomodel.point.application.service;

import com.example.nomodel._core.exception.ApplicationException;
import com.example.nomodel._core.exception.ErrorCode;
import com.example.nomodel.point.application.dto.request.PointUseRequest;
import com.example.nomodel.point.application.dto.response.PointBalanceResponse;
import com.example.nomodel.point.application.dto.response.PointTransactionResponse;
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
        // 1. 정책 위반 체크: 같은 모델에 대해 이미 보상받았는지 확인
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

        // 2. 회원 포인트 잔액 조회
        MemberPointBalance balance = pointBalanceRepository.findByMemberId(reviewerId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.MEMBER_NOT_FOUND));

        // 3. 거래 내역 저장
        PointTransaction transaction = new PointTransaction(
                reviewerId,
                TransactionDirection.CREDIT,
                TransactionType.REWARD,
                rewardAmount,
                balance.getTotalPoints(),
                balance.getTotalPoints().add(rewardAmount),
                RefererType.REVIEW,
                modelId
        );
        transactionRepository.save(transaction);

        // 4. 보류 포인트 적립
        balance.addPendingPoints(rewardAmount);
        pointBalanceRepository.save(balance);

        // 5. 5초 뒤 보류 → 가용 포인트 전환 (시연용)
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



}



