package com.example.project.analysis.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.project.analysis.domain.ActivityEvent;
import com.example.project.analysis.domain.ActivityType;

import org.junit.jupiter.api.Test;

class ContributionScoreCalculatorTest {

    private final ContributionScoreCalculator calculator = new ContributionScoreCalculator();

    @Test
    void calculatesVersionedScoreWithoutAiInput() {
        List<ActivityEvent> events = new ArrayList<>();
        events.add(event("c1", ActivityType.COMMIT, "COMMITTED", "2026-08-01T00:00:00Z", 6));
        events.add(event("c2", ActivityType.COMMIT, "COMMITTED", "2026-08-02T00:00:00Z", 4));
        events.add(event("p1", ActivityType.PULL_REQUEST, "MERGED", "2026-08-02T01:00:00Z", 0));
        events.add(event("r1", ActivityType.REVIEW, "APPROVED", "2026-08-03T00:00:00Z", 0));

        ScoreResult result = calculator.calculate(events);

        assertThat(result.metrics().commits()).isEqualTo(2);
        assertThat(result.metrics().pullRequestsMerged()).isEqualTo(1);
        assertThat(result.metrics().activeDays()).isEqualTo(3);
        assertThat(result.metrics().changedFiles()).isEqualTo(10);
        assertThat(result.score()).isEqualTo(21);
        assertThat(result.scoreVersion()).isEqualTo("score-v1");
    }

    @Test
    void capsScoreAtOneHundred() {
        List<ActivityEvent> events = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            events.add(event("c" + index, ActivityType.COMMIT, "COMMITTED",
                    "2026-08-%02dT00:00:00Z".formatted(index % 28 + 1), 10));
            events.add(event("p" + index, ActivityType.PULL_REQUEST, "MERGED",
                    "2026-08-%02dT01:00:00Z".formatted(index % 28 + 1), 0));
            events.add(event("r" + index, ActivityType.REVIEW, "APPROVED",
                    "2026-08-%02dT02:00:00Z".formatted(index % 28 + 1), 0));
        }
        assertThat(calculator.calculate(events).score()).isEqualTo(100);
    }

    private ActivityEvent event(String id, ActivityType type, String state, String instant, int files) {
        return ActivityEvent.create(null, id, type, 1L, Instant.parse(instant), id, state,
                files * 2, files, files, "{}");
    }
}
