package com.example.project.analysis.calculator;

import java.time.ZoneOffset;
import java.util.List;

import com.example.project.analysis.domain.ActivityEvent;
import com.example.project.analysis.domain.ActivityType;

import org.springframework.stereotype.Component;

@Component
public class ContributionScoreCalculator {

    public static final String SCORE_VERSION = "score-v1";
    public static final String RULES = "commit=min(count,25);pr=min(opened*4+merged*6,30);"
            + "review=min(count*4,20);activeDay=min(days,15);changedFiles=min(files/5,10)";

    public ScoreResult calculate(List<ActivityEvent> events) {
        int commits = count(events, ActivityType.COMMIT);
        int opened = count(events, ActivityType.PULL_REQUEST);
        int merged = (int) events.stream().filter(event -> event.getType() == ActivityType.PULL_REQUEST)
                .filter(event -> "MERGED".equals(event.getState())).count();
        int reviews = count(events, ActivityType.REVIEW);
        int activeDays = (int) events.stream()
                .map(event -> event.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate()).distinct().count();
        int changedFiles = events.stream().mapToInt(ActivityEvent::getChangedFiles).sum();
        int additions = events.stream().mapToInt(ActivityEvent::getAdditions).sum();
        int deletions = events.stream().mapToInt(ActivityEvent::getDeletions).sum();

        int score = Math.min(commits, 25)
                + Math.min(opened * 4 + merged * 6, 30)
                + Math.min(reviews * 4, 20)
                + Math.min(activeDays, 15)
                + Math.min(changedFiles / 5, 10);
        AnalysisMetrics metrics = new AnalysisMetrics(commits, opened, merged, reviews, activeDays,
                changedFiles, additions, deletions);
        return new ScoreResult(metrics, score, SCORE_VERSION, RULES);
    }

    private int count(List<ActivityEvent> events, ActivityType type) {
        return (int) events.stream().filter(event -> event.getType() == type).count();
    }
}
