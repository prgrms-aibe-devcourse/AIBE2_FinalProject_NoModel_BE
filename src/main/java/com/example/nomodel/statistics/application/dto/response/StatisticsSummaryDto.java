package com.example.nomodel.statistics.application.dto.response;

import lombok.AllArgsConstructor;


public class StatisticsSummaryDto {
  private long totalUsers;
  private long newUsersThisMonth;
  private long totalProjects;
  private long newProjectsThisMonth;
  private long totalSales;
  private long salesThisMonth;
  private double averageRating;
  private long totalDownloads;
  
  public StatisticsSummaryDto(long totalUsers, long newUsersThisMonth,
                              long totalProjects, long newProjectsThisMonth,
                              long totalSales, long salesThisMonth,
                              double averageRating, long totalDownloads) {
    this.totalUsers = totalUsers;
    this.newUsersThisMonth = newUsersThisMonth;
    this.totalProjects = totalProjects;
    this.newProjectsThisMonth = newProjectsThisMonth;
    this.totalSales = totalSales;
    this.salesThisMonth = salesThisMonth;
    this.averageRating = averageRating;
    this.totalDownloads = totalDownloads;
  }
  
  public long getTotalUsers() { return totalUsers; }
  public long getNewUsersThisMonth() { return newUsersThisMonth; }
  public long getTotalProjects() { return totalProjects; }
  public long getNewProjectsThisMonth() { return newProjectsThisMonth; }
  public long getTotalSales() { return totalSales; }
  public long getSalesThisMonth() { return salesThisMonth; }
  public double getAverageRating() { return averageRating; }
  public long getTotalDownloads() { return totalDownloads; }
}
