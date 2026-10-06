package com.example.project.analysis.collector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.analysis.benchmark.BenchmarkCatalog;
import com.example.project.analysis.benchmark.ActivityVector;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class GitHubActivityCollector {
    private final RepositoryActivityCollector collector;
    private final GitHubAccessTokenService tokens;
    private final ObjectMapper mapper;
    private final BenchmarkCatalog benchmarkCatalog;

    public GitHubActivityCollector(RepositoryActivityCollector collector, GitHubAccessTokenService tokens,
            ObjectMapper mapper, BenchmarkCatalog benchmarkCatalog) {
        this.collector = collector;
        this.tokens = tokens;
        this.mapper = mapper;
        this.benchmarkCatalog = benchmarkCatalog;
    }

    public CollectedSnapshot collect(AnalysisJob job, long subjectGithubId) {
        var requested = job.getRepository();
        String token = tokens.getValidAccessToken(job.getUser().getId());
        var collected = collector.collect(token,
                requested.getOwnerLogin(), requested.getName(), job.getTargetBranch(),
                job.getPeriodStart(), job.getPeriodEnd(), subjectGithubId);
        var repo = collected.repository();
        var activities = collected.activities().stream().filter(a -> a.authorGithubId() == subjectGithubId).toList();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "github");
        metadata.put("repository", repo.fullName());
        metadata.put("branch", collected.branch());
        metadata.put("defaultBranch", repo.defaultBranch());
        metadata.put("language", repo.language());
        metadata.put("publicRepository", !repo.privateRepository());
        metadata.put("fork", repo.fork());
        metadata.put("archived", repo.archived());
        metadata.put("activeContributors", collected.activities().stream().map(CollectedActivity::authorGithubId).distinct().count());
        var reference = benchmarkCatalog.latestCovered(job.getPeriodStart(), job.getPeriodEnd(), repo.language());
        if (reference != null && !repo.privateRepository() && !repo.fork() && !repo.archived()
                && collected.branch().equals(repo.defaultBranch())) {
            var referenceStart = reference.dataset().periodStart();
            var referenceEnd = reference.dataset().periodEnd();
            // Collect the reference window separately: reviews on PRs opened before this window
            // belong to the full-history snapshot but not to the published reference scope.
            var referenceCollection = collector.collect(token, requested.getOwnerLogin(), requested.getName(),
                    collected.branch(), referenceStart, referenceEnd, null);
            metadata.put("referenceDatasetId", reference.id());
            metadata.put("referenceActiveContributors", referenceCollection.activities().stream()
                    .map(CollectedActivity::authorGithubId).distinct().count());
            metadata.put("referenceMetrics", ActivityVector.from(referenceCollection.activities().stream()
                    .filter(a -> a.authorGithubId() == subjectGithubId).toList()));
        }
        metadata.put("scope", RepositoryActivityCollector.SCOPE);
        metadata.put("subjectGithubId", subjectGithubId);
        metadata.put("periodStart", job.getPeriodStart().toString());
        metadata.put("periodEnd", job.getPeriodEnd().toString());
        metadata.put("collectorVersion", RepositoryActivityCollector.VERSION);
        metadata.put("complete", true);
        metadata.put("activityCount", activities.size());
        String metadataJson = json(metadata);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("metadata", metadata);
        payload.put("activities", activities);
        return new CollectedSnapshot(Instant.now(), metadataJson, sha256(json(payload)), activities);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Cannot serialize snapshot", e); }
    }

    public static String sha256(String value) {
        try {
            return "0x" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
}
