package com.example.nomodel.statistics.application.dto.response;

public class DailyCount {
    private final int year;   // YYYY
    private final int month;  // 1~12
    private final int day;    // 1~31
    private final long count; // 건수

    public DailyCount(Integer year, Integer month, Integer day, Number count) {
        this.year = year != null ? year : 0;
        this.month = month != null ? month : 0;
        this.day = day != null ? day : 0;
        this.count = count != null ? count.longValue() : 0L;
    }

    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getDay() { return day; }
    public long getCount() { return count; }
}
