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
}
