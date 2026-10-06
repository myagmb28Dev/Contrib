package com.example.project.analysis.benchmark;

import java.time.Instant;
import java.util.List;

public record BenchmarkDataset(String schemaVersion, String collectorVersion, Instant periodStart,
        Instant periodEnd, Instant collectedAt, String scope, List<Repository> repositories) {
    public record Contributor(long githubId, ActivityVector metrics) {}
    public record Repository(String fullName, String language, String defaultBranch, boolean publicRepository,
            boolean fork, boolean archived, boolean complete, String sourceHash, List<Contributor> contributors) {}

    public static String sizeBand(int activeContributors) {
        return activeContributors < 10 ? "1-9" : activeContributors < 50 ? "10-49" : "50+";
    }
}
