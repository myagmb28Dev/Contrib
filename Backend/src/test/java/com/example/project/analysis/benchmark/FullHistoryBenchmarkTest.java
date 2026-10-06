package com.example.project.analysis.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.example.project.analysis.calculator.AnalysisMetrics;
import com.example.project.analysis.domain.RepositorySnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FullHistoryBenchmarkTest {
    private static final Instant SEPTEMBER_START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant OCTOBER_START = Instant.parse("2026-10-01T00:00:00Z");

    @Test void selectsTheLatestPublishedWindowFullyCoveredByTheFullHistory() {
        var catalog = new BenchmarkCatalog(new ObjectMapper().findAndRegisterModules(),
                "classpath*:benchmarks/*.json");
        var covered = catalog.latestCovered(Instant.parse("2026-08-14T01:02:31Z"),
                Instant.parse("2026-10-06T00:00:00Z"), "Java");
        assertThat(covered).isNotNull();
        assertThat(covered.dataset().periodStart()).isEqualTo(SEPTEMBER_START);
        assertThat(covered.dataset().periodEnd()).isEqualTo(OCTOBER_START);
        assertThat(catalog.latestCovered(SEPTEMBER_START.plusSeconds(1),
                Instant.parse("2026-10-06T00:00:00Z"), "Java")).isNull();
        assertThat(catalog.latestCovered(Instant.parse("2026-08-14T01:02:31Z"),
                Instant.parse("2026-10-06T00:00:00Z"), "Python")).isNull();
        assertThat(catalog.latestCovered(Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-10-06T00:00:00Z"), "C#").dataset().periodStart())
                .isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test void ranksTheSeparatelyCollectedReferenceWindowAndUsesItsContributorBand() throws Exception {
        var catalog = mock(BenchmarkCatalog.class);
        var dataset = PercentileCalculatorTest.dataset(false);
        var entry = new BenchmarkCatalog.Entry("data-1", dataset, "{}");
        var fullStart = Instant.parse("2026-07-01T00:00:00Z");
        var fullEnd = Instant.parse("2026-10-01T00:00:00Z");
        when(catalog.latestCovered(fullStart, fullEnd, "C#")).thenReturn(entry);
        var snapshot = mock(RepositorySnapshot.class);
        when(snapshot.getPeriodStart()).thenReturn(fullStart);
        when(snapshot.getPeriodEnd()).thenReturn(fullEnd);
        when(snapshot.getSubjectGithubId()).thenReturn(999L);
        when(snapshot.getSourceMetadata()).thenReturn("""
                {"collectorVersion":"github-v2","complete":true,"publicRepository":true,
                 "fork":false,"archived":false,"branch":"main","defaultBranch":"main",
                 "language":"C#","activeContributors":60,"referenceActiveContributors":15,
                 "referenceMetrics":{"commits":1,"pullRequestsOpened":0,"reviews":0,"activeDays":1},
                 "referenceDatasetId":"data-1"}
                """);
        var result = new BenchmarkService(catalog, new PercentileCalculator(),
                new ObjectMapper().findAndRegisterModules()).compare(snapshot,
                new AnalysisMetrics(100, 0, 0, 0, 100, 0, 0, 0));
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.modelVersion()).isEqualTo("activity-percentile-v2");
        assertThat(result.periodStart()).isEqualTo(PercentileCalculatorTest.START);
        assertThat(result.periodEnd()).isEqualTo(PercentileCalculatorTest.END);
        assertThat(result.sizeBand()).isEqualTo("10-49");
        assertThat(result.metricPercentiles().get("commits")).isEqualTo(5.0);
        assertThat(result.datasetId()).isEqualTo("data-1");
    }

    @Test void fullHistoryActivityDoesNotCreateAPercentileWhenTheReferenceWindowIsEmpty() throws Exception {
        var catalog = mock(BenchmarkCatalog.class);
        var dataset = PercentileCalculatorTest.dataset(false);
        var start = Instant.parse("2026-07-01T00:00:00Z");
        var end = Instant.parse("2026-10-01T00:00:00Z");
        when(catalog.latestCovered(start, end, "C#"))
                .thenReturn(new BenchmarkCatalog.Entry("data-1", dataset, "{}"));
        var snapshot = mock(RepositorySnapshot.class);
        when(snapshot.getPeriodStart()).thenReturn(start);
        when(snapshot.getPeriodEnd()).thenReturn(end);
        when(snapshot.getSourceMetadata()).thenReturn("""
                {"collectorVersion":"github-v2","complete":true,"publicRepository":true,
                 "fork":false,"archived":false,"branch":"main","defaultBranch":"main",
                 "language":"C#","referenceActiveContributors":15,"referenceDatasetId":"data-1",
                 "referenceMetrics":{"commits":0,"pullRequestsOpened":0,"reviews":0,"activeDays":0}}
                """);
        var result = new BenchmarkService(catalog, new PercentileCalculator(),
                new ObjectMapper().findAndRegisterModules()).compare(snapshot,
                new AnalysisMetrics(100, 20, 10, 10, 50, 0, 0, 0));
        assertThat(result.status()).isEqualTo("NO_ACTIVITY");
        assertThat(result.percentile()).isNull();
    }

}
