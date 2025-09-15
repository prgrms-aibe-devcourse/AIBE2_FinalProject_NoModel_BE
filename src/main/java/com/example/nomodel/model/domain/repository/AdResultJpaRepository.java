package com.example.nomodel.model.domain.repository;


import com.example.nomodel.model.domain.model.AdResult;
import com.example.nomodel.statistics.application.dto.response.DailyCount;
import com.example.nomodel.statistics.application.dto.response.MonthlyCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdResultJpaRepository extends JpaRepository<AdResult, Long> {
  
  // 전체 프로젝트 수
  @Query("select count(ar) from AdResult ar")
  long countAllProjects();
  
  // 기간 내 가입 프로젝 수 (예: 이번 달)
  @Query("""
           select count(ar)
             from AdResult ar
            where ar.createdAt >= :from
              and ar.createdAt <  :to
           """)
  long countProjectsJoinedBetween(@Param("from") LocalDateTime from,
                               @Param("to")   LocalDateTime to);
  
  // 특정 사용자의 프로젝트 수
  @Query("select count(ar) from AdResult ar where ar.memberId = :memberId")
  long countByMemberId(@Param("memberId") Long memberId);
  
  // 월별 생성량 (전체)
  @Query("""
           select new com.example.nomodel.statistics.application.dto.response.MonthlyCount(
                      month(a.createdAt), count(a))
             from AdResult a
            where (a.createdAt >= :from)
              and (a.createdAt <  :to)
         group by year(a.createdAt), month(a.createdAt)
         order by year(a.createdAt), month(a.createdAt)
           """)
  List<MonthlyCount> countProjectsByMonth(
          @Param("from") LocalDateTime from,
          @Param("to")   LocalDateTime to
  );

  /**
   * 최근 7일: [from, to) 범위 내 일별 프로젝트(AdResult) 생성량
   */
  @Query("""
        select new com.example.nomodel.statistics.application.dto.response.DailyCount(
                 year(a.createdAt), month(a.createdAt), day(a.createdAt), count(a)
               )
          from AdResult a
         where a.createdAt >= :from
           and a.createdAt <  :to
      group by year(a.createdAt), month(a.createdAt), day(a.createdAt)
      order by year(a.createdAt), month(a.createdAt), day(a.createdAt)
    """)
  List<DailyCount> countDailyProjects(
          @Param("from") LocalDateTime from,
          @Param("to")   LocalDateTime to
  );
  
  // 특정 회원의 모델 사용 내역 조회 (페이징)
  Page<AdResult> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId, Pageable pageable);
  
  // 특정 회원과 특정 모델의 사용 내역 조회 (페이징)
  Page<AdResult> findByMemberIdAndModelIdOrderByCreatedAtDesc(
      @Param("memberId") Long memberId,
      @Param("modelId") Long modelId,
      Pageable pageable
  );
  
  /**
   * 회원의 특정 AdResult 조회 (본인 소유 확인)
   */
  Optional<AdResult> findByIdAndMemberId(Long id, Long memberId);
  
  /**
   * 회원의 총 프로젝트 개수 조회 (countByMemberId와 동일하지만 명확한 용도)
   */
  @Query("SELECT COUNT(ar) FROM AdResult ar WHERE ar.memberId = :memberId")
  long countProjectsByMemberId(@Param("memberId") Long memberId);
  
  /**
   * 회원의 평균 평점 조회
   */
  @Query("SELECT AVG(ar.memberRating) FROM AdResult ar WHERE ar.memberId = :memberId AND ar.memberRating IS NOT NULL")
  Double findAverageRatingByMemberId(@Param("memberId") Long memberId);
  
}
