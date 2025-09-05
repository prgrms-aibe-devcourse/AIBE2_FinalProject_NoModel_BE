package com.example.nomodel.model.domain.repository;


import com.example.nomodel.model.domain.model.AdResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdResultJpaRepository extends JpaRepository<AdResult, Long> {
  
  // 전체 프로젝트 수
  @Query("select count(ar) from AdResult ar")
  long countAllProjects();
  
  // 기간 내 가입 프로젝트 수 (예: 이번 달)
  @Query("""
           select count(ar)
             from AdResult ar
            where ar.createdAt >= :from
              and ar.createdAt <  :to
           """)
  long countProjectsJoinedBetween(@Param("from") LocalDateTime from,
                               @Param("to")   LocalDateTime to);
}
