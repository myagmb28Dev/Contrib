package com.example.project.analysis.benchmark;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.project.analysis.collector.GitHubActivityCollector;
import com.example.project.analysis.collector.RepositoryActivityCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkCatalog {
    public record Entry(String id, BenchmarkDataset dataset, String json) {}
    private final List<Entry> entries;
    public BenchmarkCatalog(ObjectMapper mapper,
            @Value("${app.benchmark.location:classpath*:benchmarks/*.json}") String location) {
        List<Entry> loaded = new ArrayList<>();
        try {
            for (var resource : new PathMatchingResourcePatternResolver().getResources(location)) {
                String json;
                try (var stream = resource.getInputStream()) {
                    byte[] bytes = stream.readNBytes(10_000_001);
                    if (bytes.length > 10_000_000) throw new IllegalArgumentException("Benchmark file exceeds 10 MB");
                    json = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
                }
                var dataset = mapper.readValue(json, BenchmarkDataset.class);
                validate(dataset);
                loaded.add(new Entry(GitHubActivityCollector.sha256(json), dataset, json));
            }
            Set<String> periods = new HashSet<>();
            for (var entry : loaded) if (!periods.add(entry.dataset().periodStart() + "/" + entry.dataset().periodEnd()))
                throw new IllegalArgumentException("Multiple reference datasets for the same period");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load validated public benchmarks", e);
        }
        entries = loaded.stream().sorted(Comparator.comparing(e -> e.dataset().periodStart())).toList();
    }
    public List<Entry> entries() { return entries; }
    public Entry forPeriod(Instant start, Instant end) {
        return entries.stream().filter(e -> e.dataset().periodStart().equals(start)
                && e.dataset().periodEnd().equals(end)).findFirst().orElse(null);
    }
    public Entry latestCovered(Instant start, Instant end, String language) {
        if (language == null || language.isBlank()) return null;
        return entries.stream().filter(e -> !e.dataset().periodStart().isBefore(start)
                        && !e.dataset().periodEnd().isAfter(end)
                        && e.dataset().repositories().stream().anyMatch(r -> language.equals(r.language())))
                .max(Comparator.comparing((Entry e) -> e.dataset().periodEnd())
                        .thenComparing(e -> e.dataset().periodStart(), Comparator.reverseOrder())).orElse(null);
    }
    public static void validate(BenchmarkDataset dataset) {
        if (!"benchmark-v1".equals(dataset.schemaVersion()) || !RepositoryActivityCollector.VERSION.equals(dataset.collectorVersion())
                || !RepositoryActivityCollector.SCOPE.equals(dataset.scope()) || dataset.periodStart() == null
                || dataset.periodEnd() == null || !dataset.periodStart().isBefore(dataset.periodEnd())
                || Duration.between(dataset.periodStart(), dataset.periodEnd()).toDays() > 366
                || dataset.collectedAt() == null || dataset.collectedAt().isBefore(dataset.periodEnd())
                || dataset.repositories() == null) throw new IllegalArgumentException("Invalid benchmark schema or period");
        Set<String> repositories = new HashSet<>();
        long maxDays = java.time.temporal.ChronoUnit.DAYS.between(
                dataset.periodStart().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                dataset.periodEnd().minusNanos(1).atZone(java.time.ZoneOffset.UTC).toLocalDate()) + 1;
        for (var repository : dataset.repositories()) {
            if (repository.fullName() == null || !repository.fullName().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                    || !repositories.add(repository.fullName().toLowerCase(java.util.Locale.ROOT))
                    || !repository.publicRepository() || repository.fork() || repository.archived() || !repository.complete()
                    || repository.language() == null || repository.language().isBlank()
                    || repository.defaultBranch() == null || repository.defaultBranch().isBlank()
                    || repository.sourceHash() == null || !repository.sourceHash().matches("0x[0-9a-f]{64}")
                    || repository.contributors() == null) throw new IllegalArgumentException("Invalid reference repository");
            Set<Long> contributors = new HashSet<>();
            for (var person : repository.contributors()) {
                if (person.githubId() <= 0 || !contributors.add(person.githubId()) || person.metrics() == null
                        || !person.metrics().hasActivity() || person.metrics().activeDays() <= 0
                        || person.metrics().activeDays() > maxDays
                        || person.metrics().activeDays() > (long) person.metrics().commits() + person.metrics().pullRequestsOpened() + person.metrics().reviews())
                    throw new IllegalArgumentException("Invalid reference contributor");
            }
        }
    }
}
