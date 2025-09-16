package com.example.nomodel.point.domain.service;

import com.example.nomodel.point.domain.model.*;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PointDomainService {

    private final MemberPointBalanceRepository balanceRepository;
    private final PointTransactionRepository transactionRepository;

    public PointDomainService(MemberPointBalanceRepository balanceRepository,
                              PointTransactionRepository transactionRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    public MemberPointBalance getBalance(Long memberId) {
        return balanceRepository.findById(memberId)
                .orElse(new MemberPointBalance(memberId));
    }

    /**
     * 포인트 충전 도메인 로직 (트랜잭션 관리)
     * @param memberId 회원 ID
     * @param amount 충전 금액
     * @param paymentReference 결제 참조번호 (imp_uid)
     */
    public Mono<Void> chargePoints(Long memberId, BigDecimal amount, String paymentReference) {
        return Mono.fromRunnable(() -> {
            MemberPointBalance balance = balanceRepository.findByMemberId(memberId)
                    .orElse(new MemberPointBalance(memberId)); // 잔액이 없으면 새로 생성

            PointTransaction transaction = new PointTransaction(
                    memberId,
                    TransactionDirection.CREDIT,   // 충전은 CREDIT
                    TransactionType.CHARGE,       // 충전 타입
                    amount,
                    balance.getTotalPoints(),
                    balance.getTotalPoints().add(amount),
                    RefererType.CHARGE,           // 충전이므로 "CHARGE" 참조 타입
                    paymentReference != null ? (long) paymentReference.hashCode() : null  // 결제 참조번호를 해시값으로 저장
            );

            transactionRepository.save(transaction);

            balance.addPoints(amount);
            balanceRepository.save(balance);
        }).then();
    }
}
