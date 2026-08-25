package com.example.project.analysis.ai;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RuleBasedAiSummaryProvider implements AiSummaryProvider {

    @Override
    public String providerName() {
        return "rule-based";
    }

    @Override
    public AiSummaryResult summarize(AiSummaryInput input) {
        Set<String> areas = new LinkedHashSet<>();
        if (input.language() != null && !input.language().isBlank()) {
            areas.add(input.language());
        }
        if (input.metrics().reviews() > 0 || input.metrics().pullRequestsOpened() > 0) {
            areas.add("collaboration");
        }
        if (input.metrics().commits() > 0) {
            areas.add("implementation");
        }
        String summary = "%s에서 커밋 %d개, PR %d개, 리뷰 %d개의 활동이 확인되었습니다. 기여 점수는 %d점입니다."
                .formatted(input.repository(), input.metrics().commits(), input.metrics().pullRequestsOpened(),
                        input.metrics().reviews(), input.score());
        return new AiSummaryResult(summary, List.copyOf(areas), "rule-based-stub", "stub-v2");
    }
}
