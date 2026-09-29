package com.example.project.analysis.dto;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;

public record CreateAnalysisRequest(
        Instant periodStart,
        Instant periodEnd,
        boolean allTime) {
    public CreateAnalysisRequest(Instant periodStart, Instant periodEnd) {
        this(periodStart, periodEnd, false);
    }

    @AssertTrue(message = "기간을 지정하거나 전체 기간 분석을 선택해주세요.")
    public boolean isPeriodValid() {
        return allTime || periodStart != null && periodEnd != null && periodStart.isBefore(periodEnd);
    }
}
