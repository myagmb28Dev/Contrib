package com.example.project.analysis.benchmark;

import com.example.project.analysis.calculator.AnalysisMetrics;
import com.example.project.analysis.collector.RepositoryActivityCollector;
import com.example.project.analysis.domain.RepositorySnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkService {
    private final BenchmarkCatalog catalog;
    private final PercentileCalculator calculator;
    private final ObjectMapper mapper;
    public BenchmarkService(BenchmarkCatalog catalog, PercentileCalculator calculator, ObjectMapper mapper) {
        this.catalog = catalog;
        this.calculator = calculator;
        this.mapper = mapper;
    }
    public PercentileResult compare(RepositorySnapshot snapshot, AnalysisMetrics metrics) {
        try {
            var metadata = mapper.readTree(snapshot.getSourceMetadata());
            boolean comparable = RepositoryActivityCollector.VERSION.equals(metadata.path("collectorVersion").asText())
                    && metadata.path("complete").asBoolean(false) && metadata.path("publicRepository").asBoolean(false)
                    && !metadata.path("fork").asBoolean(true) && !metadata.path("archived").asBoolean(true)
                    && !metadata.path("branch").asText().isBlank()
                    && metadata.path("branch").asText().equals(metadata.path("defaultBranch").asText());
            var reference = catalog.latestCovered(snapshot.getPeriodStart(), snapshot.getPeriodEnd(),
                    metadata.path("language").asText(null));
            if (reference != null && comparable && (!reference.id().equals(metadata.path("referenceDatasetId").asText())
                    || !metadata.path("referenceActiveContributors").isIntegralNumber()
                    || !metadata.path("referenceMetrics").isObject())) {
                throw new IllegalStateException("Snapshot reference metadata does not match the published dataset");
            }
            var start = reference == null ? snapshot.getPeriodStart() : reference.dataset().periodStart();
            var end = reference == null ? snapshot.getPeriodEnd() : reference.dataset().periodEnd();
            int activeContributors = reference == null ? metadata.path("activeContributors").asInt(0)
                    : metadata.path("referenceActiveContributors").asInt();
            var context = new PercentileCalculator.Context(snapshot.getSubjectGithubId(), start,
                    end, metadata.path("language").asText(null), activeContributors,
                    comparable, "새 수집 기준으로 분석한 공개·비포크·비보관 저장소의 기본 브랜치만 비교할 수 있습니다.");
            var subject = reference == null || !comparable ? ActivityVector.from(metrics)
                    : mapper.treeToValue(metadata.path("referenceMetrics"), ActivityVector.class);
            return calculator.calculate(context, subject, reference == null ? null : reference.dataset(),
                    reference == null ? null : reference.id());
        } catch (Exception e) { throw new IllegalStateException("Cannot calculate activity percentile", e); }
    }
}
