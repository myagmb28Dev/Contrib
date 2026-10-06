package com.example.project.analysis.benchmark;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.project.analysis.collector.RepositoryActivityCollector;

class PercentileCalculatorTest {
    static final Instant START = Instant.parse("2026-08-01T00:00:00Z"), END = Instant.parse("2026-09-01T00:00:00Z");
    final PercentileCalculator calculator = new PercentileCalculator();
    static ActivityVector vector(int value) { return new ActivityVector(value, value, value, value); }
    static BenchmarkDataset dataset(boolean ties) {
        var repos = new ArrayList<BenchmarkDataset.Repository>();
        for (int r = 0; r < 3; r++) {
            var rows = new ArrayList<BenchmarkDataset.Contributor>();
            for (int p = 1; p <= 10; p++) rows.add(new BenchmarkDataset.Contributor(r * 10L + p, vector(ties ? 5 : p)));
            repos.add(repo("owner/repo" + r, rows));
        }
        return data(repos);
    }
    static BenchmarkDataset.Repository repo(String name, List<BenchmarkDataset.Contributor> rows) {
        return new BenchmarkDataset.Repository(name, "C#", "main", true, false, false, true, "0x" + "a".repeat(64), rows);
    }
    static BenchmarkDataset data(List<BenchmarkDataset.Repository> repos) {
        return new BenchmarkDataset("benchmark-v1", RepositoryActivityCollector.VERSION, START, END, END.plusSeconds(1),
                RepositoryActivityCollector.SCOPE, repos);
    }
    static PercentileCalculator.Context context(long id) {
        return new PercentileCalculator.Context(id, START, END, "C#", 15, true, "");
    }
    @Test void usesMidranksAndUncappedRawActivities() {
        var result = calculator.calculate(context(999), vector(5), dataset(false), "data-1");
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.percentile()).isEqualTo(45.0);
        assertThat(result.metricPercentiles().values()).containsOnly(45.0);
        assertThat(result.metricWeights().values()).containsOnly(0.25);
        assertThat(calculator.calculate(context(999), vector(100), dataset(false), "data-1").percentile()).isEqualTo(100.0);
    }
    @Test void identicalActivityIsFiftiethPercentileNotPerfectScore() {
        assertThat(calculator.calculate(context(999), vector(5), dataset(true), "data-1").percentile()).isEqualTo(50.0);
    }
    @Test void refusesMismatchedPeriodsSmallCohortsAndZeroActivity() {
        assertThat(calculator.calculate(context(999), vector(0), dataset(false), "data-1").status()).isEqualTo("NO_ACTIVITY");
        var wrongPeriod = new PercentileCalculator.Context(999, START.plusSeconds(1), END, "C#", 15, true, "");
        assertThat(calculator.calculate(wrongPeriod, vector(5), dataset(false), "data-1").status()).isEqualTo("NO_REFERENCE_PERIOD");
        var insufficient = calculator.calculate(context(999), vector(5), data(dataset(false).repositories().subList(0, 2)), "data-1");
        assertThat(insufficient.status()).isEqualTo("INSUFFICIENT_COHORT");
        assertThat(insufficient.percentile()).isNull();
        var differentLanguage = new PercentileCalculator.Context(999, START, END, "Java", 15, true, "");
        assertThat(calculator.calculate(differentLanguage, vector(5), dataset(false), "data-1").status()).isEqualTo("INSUFFICIENT_COHORT");
    }
    @Test void excludesSubjectAndDoesNotOverweightRepeatedPeople() {
        var result = calculator.calculate(context(1), vector(5), dataset(false), "data-1");
        assertThat(result.contributorCount()).isEqualTo(29);
        assertThat(result.sampleCount()).isEqualTo(29);
        var one = new ArrayList<>(dataset(false).repositories());
        var many = new ArrayList<>(dataset(false).repositories());
        for (int r = 0; r < 3; r++) {
            var rows = new ArrayList<>(many.get(r).contributors());
            rows.add(new BenchmarkDataset.Contributor(100, vector(20)));
            many.set(r, repo(many.get(r).fullName(), rows));
            if (r == 0) one.set(0, many.get(0));
        }
        var a = calculator.calculate(context(999), vector(5), data(one), "data-one");
        var b = calculator.calculate(context(999), vector(5), data(many), "data-many");
        assertThat(a.percentile()).isEqualTo(b.percentile());
        assertThat(b.contributorCount()).isEqualTo(31);
        assertThat(b.sampleCount()).isEqualTo(33);
    }
    @Test void removesUnobservedDimensionsAndRejectsPrivateScope() {
        var repos = dataset(false).repositories().stream().map(r -> repo(r.fullName(), r.contributors().stream()
                .map(p -> new BenchmarkDataset.Contributor(p.githubId(), new ActivityVector(p.metrics().commits(), 0, 0, p.metrics().activeDays()))).toList())).toList();
        var result = calculator.calculate(context(999), vector(5), data(repos), "data-1");
        assertThat(result.metricWeights()).containsOnlyKeys("commits", "activeDays");
        assertThat(result.metricWeights().values()).containsOnly(0.5);
        var privateContext = new PercentileCalculator.Context(999, START, END, "C#", 15, false, "private");
        assertThat(calculator.calculate(privateContext, vector(5), data(repos), "data-1").status()).isEqualTo("UNSUPPORTED_SCOPE");
    }
    @Test void rejectsInvalidOrDuplicatePublicReferenceRows() {
        BenchmarkCatalog.validate(dataset(false));
        var duplicate = new ArrayList<>(dataset(false).repositories());
        duplicate.add(duplicate.get(0));
        assertThatThrownBy(() -> BenchmarkCatalog.validate(data(duplicate))).isInstanceOf(IllegalArgumentException.class);
        var person = dataset(false).repositories().get(0).contributors().get(0);
        assertThatThrownBy(() -> BenchmarkCatalog.validate(data(List.of(repo("owner/demo", List.of(person, person))))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void referencePublicationChangesTheJobCacheKeyWithoutChangingCollectionSemantics() {
        String missing = com.example.project.analysis.service.AnalysisService.pipelineVersion("none");
        String published = com.example.project.analysis.service.AnalysisService.pipelineVersion("dataset-1");
        assertThat(missing).isNotEqualTo(published);
        assertThat(published).isEqualTo(com.example.project.analysis.service.AnalysisService.pipelineVersion("dataset-1"));
        assertThat(published).startsWith("github-v2-").hasSizeLessThanOrEqualTo(64);
    }
}
