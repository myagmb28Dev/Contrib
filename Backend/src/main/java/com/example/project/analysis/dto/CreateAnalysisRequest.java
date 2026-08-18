package com.example.project.analysis.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record CreateAnalysisRequest(@NotNull Instant periodStart, @NotNull Instant periodEnd) {
}
