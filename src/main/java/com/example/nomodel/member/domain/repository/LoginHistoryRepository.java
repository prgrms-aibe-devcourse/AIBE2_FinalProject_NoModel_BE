package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.LoginHistory;
import com.example.nomodel.member.domain.model.LoginStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    
    Page<LoginHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
    
    List<LoginHistory> findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long memberId, LocalDateTime after);
    
    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.hashedIp = :hashedIp " +
           "AND lh.loginStatus = :status AND lh.createdAt > :after")
    long countByHashedIpAndStatusAfter(@Param("hashedIp") String hashedIp, 
                                       @Param("status") LoginStatus status,
                                       @Param("after") LocalDateTime after);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.member.id = :memberId " +
           "AND lh.loginStatus = com.example.nomodel.member.domain.model.LoginStatus.SUCCESS " +
           "ORDER BY lh.createdAt DESC")
    List<LoginHistory> findRecentSuccessfulLogins(@Param("memberId") Long memberId, Pageable pageable);
}