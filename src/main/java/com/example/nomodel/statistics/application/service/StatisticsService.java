package com.example.nomodel.statistics.application.service;

import com.example.nomodel.member.domain.repository.MemberJpaRepository;
import com.example.nomodel.model.domain.repository.AdResultJpaRepository;
import com.example.nomodel.model.domain.repository.ModelStatisticsJpaRepository;
import com.example.nomodel.point.domain.repository.PointTransactionRepository;
import com.example.nomodel.review.domain.repository.ReviewRepository;
import com.example.nomodel.statistics.application.dto.response.StatisticsSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
}
