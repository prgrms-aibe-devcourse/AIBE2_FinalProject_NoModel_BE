package com.example.nomodel.member.domain.repository;

import com.example.nomodel.member.domain.model.Email;
import com.example.nomodel.member.domain.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(Email email);
    boolean existsByEmail(Email email);
    
    // 전체 유저 수
    @Query("select count(m) from Member m")
    long countAllUsers();
    
    // 기간 내 가입 유저 수 (예: 이번 달)
    @Query("""
           select count(m)
             from Member m
            where m.createdAt >= :from
              and m.createdAt <  :to
           """)
    long countUsersJoinedBetween(@Param("from") LocalDateTime from,
                                 @Param("to")   LocalDateTime to);
}
