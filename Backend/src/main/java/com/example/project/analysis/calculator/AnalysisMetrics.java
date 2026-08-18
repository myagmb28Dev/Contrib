package com.example.project.analysis.calculator;

public record AnalysisMetrics(
        int commits,
        int pullRequestsOpened,
        int pullRequestsMerged,
        int reviews,
        int activeDays,
        int changedFiles,
        int additions,
        int deletions) {
}
