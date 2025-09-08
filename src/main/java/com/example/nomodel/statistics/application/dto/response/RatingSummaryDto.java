package com.example.nomodel.statistics.application.dto.response;

import lombok.Getter;

@Getter
public class RatingSummaryDto {

    public long c1, c2, c3, c4, c5, total;

    public RatingSummaryDto(long c1, long c2, long c3, long c4, long c5,
                         long total) {
        this.c1 = c1; this.c2 = c2; this.c3 = c3; this.c4 = c4; this.c5 = c5;
        this.total = total;
    }
}
