package com.example.project.analysis.benchmark;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.example.project.analysis.collector.GitHubActivityCollector;
import com.example.project.analysis.collector.RepositoryActivityCollector;
import org.springframework.stereotype.Component;

@Component
public class PercentileCalculator {
    public static final String VERSION = "activity-density-v1";
    public static final int MIN_CONTRIBUTORS = 20;
    public static final int MIN_REPOSITORIES = 3;
    public static final List<String> METRICS = List.of("commits", "pullRequestsOpened", "reviews", "activeDays");
    public static final String RULES = "Full-history collection with 30-day activity density normalization; "
            + "reference dataset matching primary language and band (1-9/10-49/50+); "
            + "public non-fork non-archived default branches only; exclude subject across all reference repositories; "
            + "one contributor has total weight 1 divided across their repository observations; "
            + "metric midrank=100*(weighted below+0.5*weighted equal)/total weight; "
            + "equal weights for dimensions observed in at least max(5,ceil(20% of unique contributors)) people; "
            + "at least 2 supported dimensions; composite=mean(metric midranks); "
            + "final percentile=weighted midrank of composite within reference composites; "
            + "minimum 20 unique contributors and 3 repositories; no observed subject activity => no percentile; "
            + "merged PRs and changed files are supplementary, not additional score inputs";

    public record Context(long subjectId, Instant start, Instant end, String language, int activeContributors,
            boolean comparable, String unavailableReason) {}
    public record NormalizedVector(double commits, double pullRequestsOpened, double reviews, double activeDays) {
        public double value(String metric) {
            return switch (metric) {
                case "commits" -> commits;
                case "pullRequestsOpened" -> pullRequestsOpened;
                case "reviews" -> reviews;
                case "activeDays" -> activeDays;
                default -> throw new IllegalArgumentException("Unknown metric: " + metric);
            };
        }
    }
    private record Observation(String repository, long githubId, NormalizedVector metrics, double weight) {}

    public static double daysBetween(Instant start, Instant end) {
        if (start == null || end == null) return 30.0;
        long days = ChronoUnit.DAYS.between(start.atZone(ZoneOffset.UTC).toLocalDate(), end.atZone(ZoneOffset.UTC).toLocalDate());
        return Math.max(1.0, (double) days);
    }

    public static NormalizedVector normalize(ActivityVector raw, double days) {
        double factor = 30.0 / Math.max(1.0, days);
        double commits = raw.commits() * factor;
        double prs = raw.pullRequestsOpened() * factor;
        double reviews = raw.reviews() * factor;
        double activeDays = Math.min(30.0, raw.activeDays() * factor);
        return new NormalizedVector(commits, prs, reviews, activeDays);
    }

    public PercentileResult calculate(Context context, ActivityVector subject, BenchmarkDataset dataset, String datasetId) {
        if (!context.comparable()) return unavailable("UNSUPPORTED_SCOPE", context, dataset, datasetId, context.unavailableReason());
        if (!subject.hasActivity()) return unavailable("NO_ACTIVITY", context, dataset, datasetId, "분석 기간에 확인된 활동이 없어 백분위를 계산하지 않습니다.");
        if (dataset == null)
            return unavailable("NO_REFERENCE_PERIOD", context, null, null, "해당 언어의 공개 비교 데이터가 없습니다.");

        double refDays = daysBetween(dataset.periodStart(), dataset.periodEnd());
        double subjectDays = daysBetween(context.start(), context.end());
        NormalizedVector normalizedSubject = normalize(subject, subjectDays);

        String band = BenchmarkDataset.sizeBand(context.activeContributors());
        var matching = dataset.repositories().stream().filter(r -> r.complete() && r.publicRepository() && !r.fork() && !r.archived()
                && context.language() != null && context.language().equals(r.language())
                && band.equals(BenchmarkDataset.sizeBand(r.contributors().size()))).toList();
        List<Observation> raw = new ArrayList<>();
        for (var repository : matching) for (var contributor : repository.contributors()) {
            if (contributor.githubId() != context.subjectId())
                raw.add(new Observation(repository.fullName(), contributor.githubId(), normalize(contributor.metrics(), refDays), 0));
        }
        var occurrences = raw.stream().collect(Collectors.groupingBy(Observation::githubId, TreeMap::new, Collectors.counting()));
        var rows = raw.stream().map(r -> new Observation(r.repository(), r.githubId(), r.metrics(), 1.0 / occurrences.get(r.githubId()))).toList();
        var repositories = rows.stream().map(Observation::repository).distinct().sorted().toList();
        String cohortId = GitHubActivityCollector.sha256(datasetId + "|" + VERSION + "|" + context.language() + "|" + band
                + "|exclude=" + context.subjectId() + "|" + String.join(",", repositories));
        if (occurrences.size() < MIN_CONTRIBUTORS || repositories.size() < MIN_REPOSITORIES)
            return result("INSUFFICIENT_COHORT", context, dataset, datasetId, cohortId, null, Map.of(), Map.of(), rows.size(),
                    occurrences.size(), repositories, "동일 조건의 비교 집단이 부족합니다. 최소 3개 저장소와 20명의 기여자가 필요합니다.", null);
        int requiredSupport = Math.max(5, (int) Math.ceil(occurrences.size() * 0.2));
        var activeMetrics = METRICS.stream().filter(m -> rows.stream().filter(r -> r.metrics().value(m) > 0)
                .map(Observation::githubId).distinct().count() >= requiredSupport).toList();
        if (activeMetrics.size() < 2) return result("INSUFFICIENT_DIMENSIONS", context, dataset, datasetId, cohortId,
                null, Map.of(), Map.of(), rows.size(), occurrences.size(), repositories,
                "충분한 기여자가 활동한 비교 항목이 2개 미만이라 백분위를 계산하지 않습니다.", null);
        Map<String, Double> metrics = new LinkedHashMap<>(), weights = new LinkedHashMap<>();
        double composite = 0;
        for (String metric : activeMetrics) {
            double rank = rank(normalizedSubject.value(metric), rows, r -> r.metrics().value(metric));
            metrics.put(metric, display(rank));
            weights.put(metric, 1.0 / activeMetrics.size());
            composite += rank / activeMetrics.size();
        }
        Map<Observation, Double> referenceComposites = new LinkedHashMap<>();
        for (var row : rows) {
            double value = activeMetrics.stream().mapToDouble(m -> rank(row.metrics().value(m), rows, r -> r.metrics().value(m))).average().orElse(0);
            referenceComposites.put(row, value);
        }
        double percentile = rank(composite, rows, referenceComposites::get);
        double sensitivity = 0;
        double rawStepFactor = 30.0 / subjectDays;
        for (String metric : activeMetrics) {
            if (metric.equals("activeDays")) continue;
            double change = (rank(normalizedSubject.value(metric) + rawStepFactor, rows, r -> r.metrics().value(metric))
                    - rank(normalizedSubject.value(metric), rows, r -> r.metrics().value(metric))) / activeMetrics.size();
            sensitivity = Math.max(sensitivity, rank(composite + change, rows, referenceComposites::get) - percentile);
        }
        return result("AVAILABLE", context, dataset, datasetId, cohortId, display(percentile), metrics, weights,
                rows.size(), occurrences.size(), repositories, "선택된 공개 비교 집단 안에서의 30일 환산 활동 밀도 위치이며 개발 실력이나 품질의 평가가 아닙니다.", display(sensitivity));
    }

    private static double rank(double value, List<Observation> rows, ToDoubleFunction<Observation> metric) {
        double below = 0, equal = 0, total = 0;
        for (var row : rows) {
            double reference = metric.applyAsDouble(row);
            total += row.weight();
            if (Math.abs(reference - value) < 1e-9) equal += row.weight();
            else if (reference < value) below += row.weight();
        }
        return 100 * (below + equal / 2) / total;
    }
    private static double display(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        return value < 100 - 1e-9 && rounded == 100 ? 99.9 : rounded;
    }
    private PercentileResult unavailable(String status, Context c, BenchmarkDataset d, String id, String reason) {
        return result(status, c, d, id, null, null, Map.of(), Map.of(), 0, 0, List.of(), reason, null);
    }
    private PercentileResult result(String status, Context c, BenchmarkDataset d, String datasetId, String cohortId,
            Double percentile, Map<String, Double> metrics, Map<String, Double> weights, int count, int contributors,
            List<String> repositories, String reason, Double sensitivity) {
        return new PercentileResult(status, VERSION, percentile, metrics, weights, datasetId, cohortId,
                d == null ? null : d.collectedAt(),
                d == null ? c.start() : d.periodStart(),
                d == null ? c.end() : d.periodEnd(),
                c.language(), BenchmarkDataset.sizeBand(c.activeContributors()),
                count, contributors, repositories, RepositoryActivityCollector.SCOPE, RULES, reason, sensitivity);
    }
}
