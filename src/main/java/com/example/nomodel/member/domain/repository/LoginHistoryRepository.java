package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    
    List<LoginHistory> findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long memberId, LocalDateTime after);
}