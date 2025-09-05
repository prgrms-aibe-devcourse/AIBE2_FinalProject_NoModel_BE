package com.example.nomodel.statistics.application.service;

import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.domain.repository.AdResultJpaRepository;
import com.example.nomodel.model.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.statistics.application.dto.response.MonthlyCount;
import com.example.nomodel.statistics.application.dto.response.StatisticsMonthlyDto;
import com.example.nomodel.statistics.application.dto.response.StatisticsSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {
  
  private final MemberJpaRepository memberJpaRepository;
  private final AdResultJpaRepository adResultJpaRepository;
  private final PointTransactionRepository pointTransactionRepository;
  private final ReviewRepository reviewRepository;
  private final ModelStatisticsJpaRepository modelStatisticsJpaRepository;
  
  public StatisticsSummaryDto getStatisticsSummary() {
    ZoneId zone = ZoneId.of("Asia/Seoul");
    LocalDate first = LocalDate.now(zone).withDayOfMonth(1);
    LocalDateTime from = first.atStartOfDay();
    LocalDateTime to   = first.plusMonths(1).atStartOfDay();
    
    long totalUsers = memberJpaRepository.countAllUsers();
    long newUsersThisMonth = memberJpaRepository.countUsersJoinedBetween(from, to);

    long totalProjects = adResultJpaRepository.countAllProjects();
    long newProjectsThisMonth = adResultJpaRepository.countProjectsJoinedBetween(from, to);
    
    long totalSales = pointTransactionRepository.sumTotalSales();
    long salesThisMonth = pointTransactionRepository.sumTotalSalesJoinedBetween(from, to);
    
    long averageRating = reviewRepository.averageRating();
    long totalDownloads = modelStatisticsJpaRepository.getTotalUsageCount();
    
    return new StatisticsSummaryDto(
            totalUsers, newUsersThisMonth,
            totalProjects, newProjectsThisMonth,
            totalSales, salesThisMonth,
            averageRating, totalDownloads);
  }
  
  public List<StatisticsMonthlyDto> getStatisticsMonthly() {
    ZoneId KST = ZoneId.of("Asia/Seoul");
    LocalDate firstOfThisMonth = LocalDate.now(KST).with(TemporalAdjusters.firstDayOfMonth());
    LocalDate startMonth = firstOfThisMonth.minusMonths(11);            // 최근 12개월 시작 (과거)
    LocalDateTime from = startMonth.atStartOfDay();
    LocalDateTime to   = firstOfThisMonth.plusMonths(1).atStartOfDay(); // 다음달 1일 00:00 (미포함)
    
    // Repository 호출 (월별 집계)
    List<MonthlyCount> projects = adResultJpaRepository.countProjectsByMonth(from, to);
    List<MonthlyCount> revenues = pointTransactionRepository.sumRevenueByMonth(from, to);
    
    // 월 → 값 맵으로 변환 (동일 월에 중복이 들어올 여지를 Long::sum으로 안전 처리)
    Map<Integer, Long> projectMap = projects.stream()
            .collect(java.util.stream.Collectors.toMap(
                    MonthlyCount::getMonth,
                    MonthlyCount::getCount,
                    Long::sum
            ));
    Map<Integer, Long> revenueMap = revenues.stream()
            .collect(java.util.stream.Collectors.toMap(
                    MonthlyCount::getMonth,
                    MonthlyCount::getCount,
                    Long::sum
            ));
    
    // 최근 12개월을 순서대로 순회하면서 결과 구성 (없으면 0)
    List<StatisticsMonthlyDto> result = new ArrayList<>(12);
    LocalDate cursor = startMonth;
    for (int i = 0; i < 12; i++) {
      int m = cursor.getMonthValue(); // 1~12
      long p = projectMap.getOrDefault(m, 0L);
      long r = revenueMap.getOrDefault(m, 0L);
      result.add(new StatisticsMonthlyDto(m + "월", p, r));
      cursor = cursor.plusMonths(1);
    }
    return result;
  }
  
}
