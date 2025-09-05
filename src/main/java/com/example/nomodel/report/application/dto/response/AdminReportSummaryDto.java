package com.example.nomodel.report.application.dto.response;

public class AdminReportSummaryDto {
  private final Long total;
  private final Long pending;
  private final Long inProgress;
  private final Long resolved;
  private final Long rejected;
  
  public AdminReportSummaryDto(Long total, Long pending,
                               Long inProgress, Long resolved, Long rejected) {
    this.total = total;
    this.pending = pending;
    this.inProgress = inProgress;
    this.resolved = resolved;
    this.rejected = rejected;
  }
  
  public Long getTotal()       { return total; }
  public Long getPending()     { return pending; }
  public Long getInProgress()  { return inProgress; }
  public Long getResolved()    { return resolved; }
  public Long getRejected()    { return rejected; }
}
