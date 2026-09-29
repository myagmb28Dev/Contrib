package com.example.project.analysis.benchmark;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PercentileResult(String status, String modelVersion, Double percentile,
        Map<String, Double> metricPercentiles, Map<String, Double> metricWeights,
        String datasetId, String cohortId, Instant referenceCollectedAt, Instant periodStart, Instant periodEnd,
        String language, String sizeBand, int sampleCount, int contributorCount, List<String> repositories,
        String scope, String calculationRules, String reason, Double singleActivitySensitivity) {}
