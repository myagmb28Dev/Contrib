package com.example.project.analysis.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import com.example.project.analysis.collector.GitHubActivityCollector;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Reproducible offline validation using the production percentile calculator and saved public aggregates. */
public class BenchmarkValidation {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) throw new IllegalArgumentException("Usage: DATASET ...");
        var mapper = new ObjectMapper().findAndRegisterModules();
        var calculator = new PercentileCalculator();
        System.out.println("# Public benchmark validation\n");
        System.out.println("Reference observations are evaluated with each subject excluded. These are activity ranks, not quality labels.\n");
        for (String file : args) {
            String json = Files.readString(Path.of(file)).replace("\r\n", "\n");
            var data = mapper.readValue(json, BenchmarkDataset.class);
            BenchmarkCatalog.validate(data);
            String id = GitHubActivityCollector.sha256(json);
            System.out.println("## " + data.periodStart() + " to " + data.periodEnd() + " (end excluded)\n");
            System.out.println("Dataset: `" + id + "`\n");
            System.out.println("| Repository | Language | Active contributors |\n|---|---|---:|");
            Map<String, Integer> statuses = new TreeMap<>();
            var percentiles = new ArrayList<Double>();
            double largestCommitIncrease = 0;
            int monotonicityViolations = 0;
            for (var repo : data.repositories()) {
                System.out.println("| " + repo.fullName() + " | " + repo.language() + " | " + repo.contributors().size() + " |");
                for (var person : repo.contributors()) {
                    var context = new PercentileCalculator.Context(person.githubId(), data.periodStart(), data.periodEnd(),
                            repo.language(), repo.contributors().size(), true, "");
                    var result = calculator.calculate(context, person.metrics(), data, id);
                    statuses.merge(result.status(), 1, Integer::sum);
                    if (result.percentile() != null) {
                        percentiles.add(result.percentile());
                        var original = person.metrics();
                        var changed = new ActivityVector(original.commits() + 1, original.pullRequestsOpened(), original.reviews(), original.activeDays());
                        double delta = calculator.calculate(context, changed, data, id).percentile() - result.percentile();
                        if (delta < 0) monotonicityViolations++;
                        largestCommitIncrease = Math.max(largestCommitIncrease, delta);
                    }
                }
            }
            System.out.println("\nResult counts: " + statuses + "\n");
            if (!percentiles.isEmpty()) {
                percentiles.sort(Double::compareTo);
                System.out.printf(java.util.Locale.ROOT, "Observed percentile range: %.1f to %.1f; middle observation: %.1f.\n\n",
                        percentiles.get(0), percentiles.get(percentiles.size() - 1), percentiles.get(percentiles.size() / 2));
                System.out.println("| Percentile band | Observations |\n|---|---:|");
                for (int lower = 0; lower < 100; lower += 20) {
                    final int bound = lower;
                    long count = percentiles.stream().filter(p -> p >= bound && (p < bound + 20 || bound == 80)).count();
                    System.out.println("| " + lower + "-" + (lower + 20) + " | " + count + " |");
                }
                System.out.printf(java.util.Locale.ROOT, "\nLargest change after adding one commit (other dimensions fixed): %.1f percentile points.\n\n", largestCommitIncrease);
            }
            System.out.println("Monotonicity violations after adding one commit: " + monotonicityViolations + ".\n");
            if (monotonicityViolations != 0) throw new IllegalStateException("Monotonicity regression");
        }
        System.out.println("Limits: curated convenience sample, not a representative population; repository size and language do not control developer role or workflow. "
                + "Small samples and ties can produce large rank jumps. The equal-weight model remains experimental and requires broader calibration before use in high-stakes evaluation.");
    }
}
