package com.example.project.analysis.benchmark;

import java.time.ZoneOffset;
import java.util.List;
import com.example.project.analysis.calculator.AnalysisMetrics;
import com.example.project.analysis.collector.CollectedActivity;
import com.example.project.analysis.domain.ActivityType;

public record ActivityVector(int commits, int pullRequestsOpened, int reviews, int activeDays) {
    public ActivityVector {
        if (commits < 0 || pullRequestsOpened < 0 || reviews < 0 || activeDays < 0)
            throw new IllegalArgumentException("Activity counts must be nonnegative");
    }
    public static ActivityVector from(AnalysisMetrics m) {
        return new ActivityVector(m.commits(), m.pullRequestsOpened(), m.reviews(), m.activeDays());
    }
    public static ActivityVector from(List<CollectedActivity> events) {
        return new ActivityVector(count(events, ActivityType.COMMIT), count(events, ActivityType.PULL_REQUEST),
                count(events, ActivityType.REVIEW), (int) events.stream()
                        .map(e -> e.occurredAt().atZone(ZoneOffset.UTC).toLocalDate()).distinct().count());
    }
    private static int count(List<CollectedActivity> events, ActivityType type) {
        return (int) events.stream().filter(e -> e.type() == type).count();
    }
    public int value(String metric) {
        return switch (metric) {
            case "commits" -> commits;
            case "pullRequestsOpened" -> pullRequestsOpened;
            case "reviews" -> reviews;
            case "activeDays" -> activeDays;
            default -> throw new IllegalArgumentException("Unknown metric: " + metric);
        };
    }
    public boolean hasActivity() { return commits > 0 || pullRequestsOpened > 0 || reviews > 0; }
}
