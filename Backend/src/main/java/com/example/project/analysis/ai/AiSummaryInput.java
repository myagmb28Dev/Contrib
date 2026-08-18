package com.example.project.analysis.ai;

import com.example.project.analysis.calculator.AnalysisMetrics;

public record AiSummaryInput(String repository, String language, AnalysisMetrics metrics, int score) {
}
