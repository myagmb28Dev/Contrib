package com.example.project.analysis.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.project.analysis.collector.RepositoryActivityCollector;
import org.junit.jupiter.api.Test;

class PercentileCalculatorEdgeCaseTest {

    private static final Instant START = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-09-01T00:00:00Z");
    private final PercentileCalculator calculator = new PercentileCalculator();

    private static ActivityVector vector(int val) {
        return new ActivityVector(val, val, val, val);
    }

    private static BenchmarkDataset.Repository repo(String name, List<BenchmarkDataset.Contributor> rows) {
        return new BenchmarkDataset.Repository(name, "Java", "main", true, false, false, true, "0x" + "a".repeat(64), rows);
    }

    private static BenchmarkDataset data(List<BenchmarkDataset.Repository> repos) {
        return new BenchmarkDataset("dataset-edge", RepositoryActivityCollector.VERSION, START, END, END.plusSeconds(1),
                RepositoryActivityCollector.SCOPE, repos);
    }

    private static PercentileCalculator.Context context(long id, int contributors) {
        return new PercentileCalculator.Context(id, START, END, "Java", contributors, true, "");
    }

    @Test
    void rejectsWhenTotalContributorsBelowMinimumThreshold() {
        List<BenchmarkDataset.Contributor> repo1 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo2 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo3 = new ArrayList<>();

        for (int i = 1; i <= 7; i++) repo1.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 8; i <= 14; i++) repo2.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 15; i <= 19; i++) repo3.add(new BenchmarkDataset.Contributor(i, vector(i)));

        BenchmarkDataset dataset = data(List.of(repo("owner/r1", repo1), repo("owner/r2", repo2), repo("owner/r3", repo3)));
        PercentileResult result = calculator.calculate(context(999, 5), vector(10), dataset, "edge-1");

        assertThat(result.status()).isEqualTo("INSUFFICIENT_COHORT");
        assertThat(result.percentile()).isNull();
        assertThat(result.contributorCount()).isEqualTo(19);
    }

    @Test
    void succeedsWhenTotalContributorsReachesExactMinimumThreshold() {
        List<BenchmarkDataset.Contributor> repo1 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo2 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo3 = new ArrayList<>();

        for (int i = 1; i <= 7; i++) repo1.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 8; i <= 14; i++) repo2.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 15; i <= 20; i++) repo3.add(new BenchmarkDataset.Contributor(i, vector(i)));

        BenchmarkDataset dataset = data(List.of(repo("owner/r1", repo1), repo("owner/r2", repo2), repo("owner/r3", repo3)));
        PercentileResult result = calculator.calculate(context(999, 5), vector(10), dataset, "edge-2");

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.percentile()).isNotNull();
        assertThat(result.contributorCount()).isEqualTo(20);
        assertThat(result.repositories()).hasSize(3);
    }

    @Test
    void rejectsWhenRepositoryCountBelowMinimumEvenWithSufficientContributors() {
        List<BenchmarkDataset.Contributor> repo1 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo2 = new ArrayList<>();

        for (int i = 1; i <= 15; i++) repo1.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 16; i <= 30; i++) repo2.add(new BenchmarkDataset.Contributor(i, vector(i)));

        BenchmarkDataset dataset = data(List.of(repo("owner/r1", repo1), repo("owner/r2", repo2)));
        PercentileResult result = calculator.calculate(context(999, 15), vector(10), dataset, "edge-3");

        assertThat(result.status()).isEqualTo("INSUFFICIENT_COHORT");
        assertThat(result.percentile()).isNull();
        assertThat(result.repositories()).hasSize(2);
    }

    @Test
    void isolatesDifferentSizeBands() {
        List<BenchmarkDataset.Contributor> small1 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> small2 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> small3 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) small1.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 6; i <= 10; i++) small2.add(new BenchmarkDataset.Contributor(i, vector(i)));
        for (int i = 11; i <= 15; i++) small3.add(new BenchmarkDataset.Contributor(i, vector(i)));

        BenchmarkDataset dataset = data(List.of(repo("owner/s1", small1), repo("owner/s2", small2), repo("owner/s3", small3)));
        PercentileResult result = calculator.calculate(context(999, 25), vector(5), dataset, "edge-4");

        assertThat(result.status()).isEqualTo("INSUFFICIENT_COHORT");
    }

    @Test
    void correctlyComputesZeroSensitivityWhenAllReferenceMetricsEqual() {
        List<BenchmarkDataset.Contributor> repo1 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo2 = new ArrayList<>();
        List<BenchmarkDataset.Contributor> repo3 = new ArrayList<>();
        for (int i = 1; i <= 10; i++) repo1.add(new BenchmarkDataset.Contributor(i, vector(5)));
        for (int i = 11; i <= 20; i++) repo2.add(new BenchmarkDataset.Contributor(i, vector(5)));
        for (int i = 21; i <= 30; i++) repo3.add(new BenchmarkDataset.Contributor(i, vector(5)));

        BenchmarkDataset dataset = data(List.of(repo("owner/r1", repo1), repo("owner/r2", repo2), repo("owner/r3", repo3)));
        PercentileResult result = calculator.calculate(context(999, 15), vector(5), dataset, "edge-5");

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.percentile()).isEqualTo(50.0);
    }
}
