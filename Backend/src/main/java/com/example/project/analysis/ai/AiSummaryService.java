package com.example.project.analysis.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiSummaryService {

    private final RuleBasedAiSummaryProvider fallback;
    private final List<AiSummaryProvider> providers;
    private final String providerName;

    public AiSummaryService(RuleBasedAiSummaryProvider fallback, List<AiSummaryProvider> providers,
            @Value("${app.ai.provider:rule-based}") String providerName) {
        this.fallback = fallback;
        this.providers = providers;
        this.providerName = providerName;
    }

    public AiSummaryResult summarize(AiSummaryInput input) {
        AiSummaryProvider selected = providers.stream()
                .filter(provider -> provider.providerName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElse(fallback);
        try {
            return selected.summarize(input);
        } catch (RuntimeException exception) {
            return fallback.summarize(input);
        }
    }
}
