package com.example.project.analysis.service;

import java.util.UUID;

import com.example.project.analysis.ai.AiSummaryInput;

public record CalculatedAnalysis(UUID analysisId, AiSummaryInput aiInput) {
}
