package com.example.project.analysis.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.project.analysis.calculator.AnalysisMetrics;
import java.util.List;

import org.junit.jupiter.api.Test;

class RuleBasedAiSummaryProviderTest {

    @Test
    void providesDeterministicFallbackWithoutChangingScore() {
        RuleBasedAiSummaryProvider provider = new RuleBasedAiSummaryProvider();
        AnalysisMetrics metrics = new AnalysisMetrics(3, 1, 1, 2, 3, 8, 100, 20);

        AiSummaryResult first = provider.summarize(new AiSummaryInput("owner/repo", "Java", metrics, 42));
        AiSummaryResult second = provider.summarize(new AiSummaryInput("owner/repo", "Java", metrics, 42));

        assertThat(first).isEqualTo(second);
        assertThat(first.model()).isEqualTo("rule-based-stub");
        assertThat(first.summary()).contains("42점");
        assertThat(first.technicalAreas()).containsExactly("Java", "collaboration", "implementation");
    }

    @Test
    void fallsBackWhenConfiguredProviderFails() {
        RuleBasedAiSummaryProvider fallback = new RuleBasedAiSummaryProvider();
        AiSummaryProvider failing = new AiSummaryProvider() {
            @Override public String providerName() { return "openai"; }
            @Override public AiSummaryResult summarize(AiSummaryInput input) {
                throw new IllegalStateException("temporary outage");
            }
        };
        AiSummaryService service = new AiSummaryService(fallback, List.of(fallback, failing), "openai");
        AnalysisMetrics metrics = new AnalysisMetrics(1, 0, 0, 0, 1, 1, 1, 0);

        assertThat(service.summarize(new AiSummaryInput("owner/repo", "Java", metrics, 2)).model())
                .isEqualTo("rule-based-stub");
    }
}
