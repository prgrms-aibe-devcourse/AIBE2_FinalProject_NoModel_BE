package com.example.nomodel.point.domain.service;

import com.example.nomodel.point.domain.model.*;
import com.example.nomodel.point.domain.repository.MemberPointBalanceRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import org.springframework.stereotype.Service;

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

    public PointTransaction addPoints(Long memberId, BigDecimal amount, TransactionType type, RefererType refererType, Long refererId) {
        MemberPointBalance balance = getBalance(memberId);
        BigDecimal before = balance.getAvailablePoints();
        balance.addPoints(amount);
        MemberPointBalance saved = balanceRepository.save(balance);

        PointTransaction tx = new PointTransaction(
                memberId, TransactionDirection.CREDIT, type,
                amount, before, saved.getAvailablePoints(),
                refererType, refererId
        );
        return transactionRepository.save(tx);
    }

    public PointTransaction usePoints(Long memberId, BigDecimal amount, TransactionType type, RefererType refererType, Long refererId) {
        MemberPointBalance balance = getBalance(memberId);
        BigDecimal before = balance.getAvailablePoints();
        balance.subtractPoints(amount);
        MemberPointBalance saved = balanceRepository.save(balance);

        PointTransaction tx = new PointTransaction(
                memberId, TransactionDirection.DEBIT, type,
                amount, before, saved.getAvailablePoints(),
                refererType, refererId
        );
        return transactionRepository.save(tx);
    }

    public List<PointTransaction> getTransactions(Long memberId) {
        return transactionRepository.findByMemberId(memberId);
    }
}
