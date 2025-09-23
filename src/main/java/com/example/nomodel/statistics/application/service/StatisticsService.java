package com.example.nomodel.statistics.application.service;

import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.command.domain.repository.AdResultJpaRepository;
import com.example.nomodel.model.command.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.statistics.application.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static java.lang.Math.round;
import static java.util.Calendar.*;

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

  public List<DailyActivityDto> getDailyActivity() {

    // 1) 최근 7일 범위(KST): [6일 전 00:00, 내일 00:00)
    ZoneId KST = ZoneId.of("Asia/Seoul");
    LocalDate today = LocalDate.now(KST);
    LocalDate start = today.minusDays(6);

    LocalDateTime from = start.atStartOfDay();
    LocalDateTime to   = today.plusDays(1).atStartOfDay();

    // 2) JPQL 집계
    List<DailyCount> signups  = memberJpaRepository.countDailySignups(from, to);
    List<DailyCount> projects = adResultJpaRepository.countDailyProjects(from, to);

    // 3) yyyy-MM-dd 키로 맵핑
    Map<LocalDate, Long> uMap = new HashMap<>();
    for (DailyCount d : signups) {
      uMap.put(LocalDate.of(d.getYear(), d.getMonth(), d.getDay()), d.getCount());
    }
    Map<LocalDate, Long> pMap = new HashMap<>();
    for (DailyCount d : projects) {
      pMap.put(LocalDate.of(d.getYear(), d.getMonth(), d.getDay()), d.getCount());
    }

    // 4) 최근 7일을 순회하며 0 보간 + 요일 한글 생성
    List<DailyActivityDto> result = new ArrayList<>(7);
    LocalDate cur = start;
    for (int i=0; i<7; i++) {
      long users = uMap.getOrDefault(cur, 0L);
      long projs = pMap.getOrDefault(cur, 0L);
      String dayKo = toKoreanDow(cur.getDayOfWeek());
      result.add(new DailyActivityDto(dayKo, users, projs));
      cur = cur.plusDays(1);
    }

    return result;
  }

  private static String toKoreanDow(DayOfWeek d) {
    return switch (d) {
      case MONDAY -> "월";
      case TUESDAY -> "화";
      case WEDNESDAY -> "수";
      case THURSDAY -> "목";
      case FRIDAY -> "금";
      case SATURDAY -> "토";
      case SUNDAY -> "일";
    };
  }

  public List<RatingDistributionDto> getRatingDistribution() {

    RatingSummaryDto s = reviewRepository.getRatingSummary();

    List<RatingDistributionDto> result = new ArrayList<>();
    long[] counts = new long[]{0, s.getC1(), s.getC2(), s.getC3(), s.getC4(), s.getC5()};
    for(int i=1; i<=5; i++) {
      double pct = s.getTotal() == 0 ? 0d : counts[i] * 100.0 / s.getTotal();
      RatingDistributionDto item = new RatingDistributionDto(i, counts[i], Math.round(pct));
      result.add(item);
    }
    return result;
  }
}
