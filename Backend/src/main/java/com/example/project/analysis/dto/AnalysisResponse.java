package com.example.project.analysis.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.project.analysis.domain.ContributionAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record AnalysisResponse(
        UUID id,
        UUID jobId,
        UUID repositoryId,
        String repositoryName,
        String repositoryFullName,
        Instant periodStart,
        Instant periodEnd,
        JsonNode metrics,
        int score,
        String scoreVersion,
        String calculationRules,
        JsonNode technicalAreas,
        String summary,
        String aiModel,
        String aiPromptVersion) {

    public static AnalysisResponse from(ContributionAnalysis analysis, ObjectMapper objectMapper) {
        var snapshot = analysis.getSnapshot();
        var repo = snapshot.getRepository();
        return new AnalysisResponse(analysis.getId(), snapshot.getAnalysisJob().getId(),
                repo.getId(), repo.getName(), repo.getFullName(),
                snapshot.getPeriodStart(), snapshot.getPeriodEnd(),
                read(objectMapper, analysis.getMetrics()), analysis.getScore(), analysis.getScoreVersion(),
                analysis.getCalculationRules(), read(objectMapper, analysis.getTechnicalAreas()),
                analysis.getAiSummary(), analysis.getAiModel(), analysis.getAiPromptVersion());
    }

    private static JsonNode read(ObjectMapper objectMapper, String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored analysis JSON is invalid", exception);
        }
    }
}
