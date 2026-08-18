package com.example.project.analysis.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.project.analysis.domain.AnalysisJob;

public record AnalysisJobResponse(
        UUID id,
        UUID repositoryId,
        Instant periodStart,
        Instant periodEnd,
        String collectorVersion,
        String status,
        int progress,
        String errorCode,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {

    public static AnalysisJobResponse from(AnalysisJob job) {
        return new AnalysisJobResponse(job.getId(), job.getRepository().getId(), job.getPeriodStart(),
                job.getPeriodEnd(), job.getCollectorVersion(), job.getStatus().name(), job.getProgress(),
                job.getErrorCode(), job.getErrorMessage(), job.getStartedAt(), job.getCompletedAt());
    }
}
