package com.example.project.analysis.ai;

public interface AiSummaryProvider {
    String providerName();
    AiSummaryResult summarize(AiSummaryInput input);
}
