package com.example.nomodel.statistics.application.dto.response;

public class MonthlyCount {
  private int year;
  private int month; // 1~12
  private long count;
  
  public MonthlyCount(int month, long count) {
    this.month = month;
    this.count = count;
  }
  
  public MonthlyCount(int year, int month, long count) {
    this.month = month;
    this.count = count;
  }
  
  public int getMonth() { return month; }
  public long getCount() { return count; }
}
