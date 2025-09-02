package com.example.nomodel.point.domain.repository;

import com.example.nomodel.point.domain.model.MemberPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPointBalanceRepository extends JpaRepository<MemberPointBalance, Long> {
}
