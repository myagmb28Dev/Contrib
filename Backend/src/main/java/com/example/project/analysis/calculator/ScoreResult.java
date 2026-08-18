package com.example.project.analysis.calculator;

public record ScoreResult(AnalysisMetrics metrics, int score, String scoreVersion, String calculationRules) {
}
