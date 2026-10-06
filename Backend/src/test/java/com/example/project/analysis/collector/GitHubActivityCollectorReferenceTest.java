package com.example.project.analysis.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.project.analysis.benchmark.BenchmarkCatalog;
import com.example.project.analysis.benchmark.BenchmarkDataset;
import com.example.project.analysis.domain.ActivityType;
import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.auth.domain.User;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.repository.domain.GitHubRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GitHubActivityCollectorReferenceTest {
    @Test void independentlyCollectsTheReferenceWindowSoOlderPrReviewsAreNotCounted() throws Exception {
        var start = Instant.parse("2026-08-14T01:02:31Z");
        var end = Instant.parse("2026-10-06T00:00:00Z");
        var referenceStart = Instant.parse("2026-09-01T00:00:00Z");
        var referenceEnd = Instant.parse("2026-10-01T00:00:00Z");
        var repository = mock(GitHubRepository.class);
        when(repository.getOwnerLogin()).thenReturn("owner");
        when(repository.getName()).thenReturn("repo");
        var user = mock(User.class);
        var userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        var job = mock(AnalysisJob.class);
        when(job.getRepository()).thenReturn(repository);
        when(job.getUser()).thenReturn(user);
        when(job.getPeriodStart()).thenReturn(start);
        when(job.getPeriodEnd()).thenReturn(end);
        when(job.getTargetBranch()).thenReturn("main");
        var tokens = mock(GitHubAccessTokenService.class);
        when(tokens.getValidAccessToken(userId)).thenReturn("token");
        var catalog = mock(BenchmarkCatalog.class);
        var dataset = new BenchmarkDataset("benchmark-v1", RepositoryActivityCollector.VERSION,
                referenceStart, referenceEnd, referenceEnd, RepositoryActivityCollector.SCOPE, List.of());
        when(catalog.latestCovered(start, end, "Java"))
                .thenReturn(new BenchmarkCatalog.Entry("reference-1", dataset, "{}"));
        var source = new GitHubRepositoryDto(1, null, "repo", "owner/repo", "https://github.com/owner/repo",
                false, "main", "Java", false);
        var reviewOnOlderPr = new CollectedActivity("review-1", ActivityType.REVIEW, 123,
                referenceStart.plusSeconds(10), "Review on #1", "APPROVED", 0, 0, 0, "{}");
        var commit = new CollectedActivity("commit-1", ActivityType.COMMIT, 123,
                referenceStart.plusSeconds(20), "Commit", "COMMITTED", 0, 0, 0, "{}");
        var peerCommit = new CollectedActivity("commit-2", ActivityType.COMMIT, 456,
                referenceStart.plusSeconds(30), "Peer", "COMMITTED", 0, 0, 0, "{}");
        var collector = mock(RepositoryActivityCollector.class);
        when(collector.collect("token", "owner", "repo", "main", start, end, 123L))
                .thenReturn(new RepositoryActivityCollector.Collection(source, "main", List.of(reviewOnOlderPr, commit)));
        when(collector.collect("token", "owner", "repo", "main", referenceStart, referenceEnd, null))
                .thenReturn(new RepositoryActivityCollector.Collection(source, "main", List.of(commit, peerCommit)));
        var snapshot = new GitHubActivityCollector(collector, tokens, new ObjectMapper().findAndRegisterModules(), catalog)
                .collect(job, 123L);
        var metadata = new ObjectMapper().readTree(snapshot.sourceMetadata());
        assertThat(metadata.path("referenceDatasetId").asText()).isEqualTo("reference-1");
        assertThat(metadata.path("referenceActiveContributors").asInt()).isEqualTo(2);
        assertThat(metadata.path("referenceMetrics").path("commits").asInt()).isEqualTo(1);
        assertThat(metadata.path("referenceMetrics").path("reviews").asInt()).isZero();
        assertThat(snapshot.activities()).hasSize(2);
        verify(collector).collect("token", "owner", "repo", "main", referenceStart, referenceEnd, null);
    }
}
