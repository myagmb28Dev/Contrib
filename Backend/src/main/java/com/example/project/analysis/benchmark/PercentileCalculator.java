package com.example.project.analysis.benchmark;

import java.time.Instant;
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
    public static final String VERSION = "activity-percentile-v2";
    public static final int MIN_CONTRIBUTORS = 20;
    public static final int MIN_REPOSITORIES = 3;
    public static final List<String> METRICS = List.of("commits", "pullRequestsOpened", "reviews", "activeDays");
    public static final String RULES = "Full-history collection; percentile uses a fully covered closed UTC reference [start,end) "
            + "and subject activity and active-contributor band from only that period; same primary language and band (1-9/10-49/50+); "
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
    private record Observation(String repository, long githubId, ActivityVector metrics, double weight) {}

    public PercentileResult calculate(Context context, ActivityVector subject, BenchmarkDataset dataset, String datasetId) {
        if (!context.comparable()) return unavailable("UNSUPPORTED_SCOPE", context, dataset, datasetId, context.unavailableReason());
        if (!subject.hasActivity()) return unavailable("NO_ACTIVITY", context, dataset, datasetId, "분석 기간에 확인된 활동이 없어 백분위를 계산하지 않습니다.");
        if (dataset == null || !dataset.periodStart().equals(context.start()) || !dataset.periodEnd().equals(context.end()))
            return unavailable("NO_REFERENCE_PERIOD", context, null, null, "전체 분석 기간에 포함되는 공개 비교 데이터가 없습니다.");
        String band = BenchmarkDataset.sizeBand(context.activeContributors());
        var matching = dataset.repositories().stream().filter(r -> r.complete() && r.publicRepository() && !r.fork() && !r.archived()
                && context.language() != null && context.language().equals(r.language())
                && band.equals(BenchmarkDataset.sizeBand(r.contributors().size()))).toList();
        List<Observation> raw = new ArrayList<>();
        for (var repository : matching) for (var contributor : repository.contributors()) {
            if (contributor.githubId() != context.subjectId())
                raw.add(new Observation(repository.fullName(), contributor.githubId(), contributor.metrics(), 0));
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
            double rank = rank(subject.value(metric), rows, r -> r.metrics().value(metric));
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
        for (String metric : activeMetrics) {
            if (metric.equals("activeDays")) continue;
            double change = (rank((double) subject.value(metric) + 1, rows, r -> r.metrics().value(metric))
                    - rank(subject.value(metric), rows, r -> r.metrics().value(metric))) / activeMetrics.size();
            sensitivity = Math.max(sensitivity, rank(composite + change, rows, referenceComposites::get) - percentile);
        }
        return result("AVAILABLE", context, dataset, datasetId, cohortId, display(percentile), metrics, weights,
                rows.size(), occurrences.size(), repositories, "선택된 공개 비교 집단 안에서의 활동 위치이며 개발 실력이나 품질의 평가가 아닙니다.", display(sensitivity));
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
                d == null ? null : d.collectedAt(), c.start(), c.end(), c.language(), BenchmarkDataset.sizeBand(c.activeContributors()),
                count, contributors, repositories, RepositoryActivityCollector.SCOPE, RULES, reason, sensitivity);
    }
}
