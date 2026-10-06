package com.example.project.analysis.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.stream.Collectors;
import com.example.project.analysis.collector.CollectedActivity;
import com.example.project.analysis.collector.GitHubActivityCollector;
import com.example.project.analysis.collector.RepositoryActivityCollector;
import com.example.project.github.client.GitHubApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.web.client.RestClient;

/** Explicit offline collection only: no production database, no export of private repository data. */
public class BenchmarkExport {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) throw new IllegalArgumentException("Usage: START END OUTPUT OWNER/REPO ...");
        var start = Instant.parse(args[0]);
        var end = Instant.parse(args[1]);
        if (!start.isBefore(end) || end.isAfter(Instant.now())) throw new IllegalArgumentException("Use a closed period");
        String token = System.getenv("GH_TOKEN");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("GH_TOKEN is required");
        var mapper = new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var api = new GitHubApiClient(RestClient.builder(), 3, Duration.ofSeconds(1), Duration.ofSeconds(30));
        var collector = new RepositoryActivityCollector(api, mapper);
        var repositories = new ArrayList<BenchmarkDataset.Repository>();
        for (int i = 3; i < args.length; i++) {
            String[] name = args[i].split("/");
            if (name.length != 2) throw new IllegalArgumentException("Invalid repository");
            var metadata = api.getRepository(token, name[0], name[1]);
            if (metadata.privateRepository() || metadata.fork() || metadata.archived())
                throw new IllegalArgumentException("Only public, non-fork, active repositories may be exported");
            System.out.println("Collecting public reference " + metadata.fullName());
            var collection = collector.collect(token, name[0], name[1], metadata.defaultBranch(), start, end, null);
            var contributors = collection.activities().stream().collect(Collectors.groupingBy(CollectedActivity::authorGithubId,
                    TreeMap::new, Collectors.toList())).entrySet().stream()
                    .map(e -> new BenchmarkDataset.Contributor(e.getKey(), ActivityVector.from(e.getValue()))).toList();
            var evidence = collection.activities().stream().map(a -> a.type() + ":" + a.externalId() + ":"
                    + a.authorGithubId() + ":" + a.occurredAt() + ":" + a.state()).toList();
            repositories.add(new BenchmarkDataset.Repository(metadata.fullName(), metadata.language(), metadata.defaultBranch(),
                    true, false, false, true, GitHubActivityCollector.sha256(mapper.writeValueAsString(evidence)), contributors));
            System.out.println("  " + contributors.size() + " active contributors; " + collection.activities().size() + " unique activities");
        }
        repositories.sort(Comparator.comparing(BenchmarkDataset.Repository::fullName));
        var dataset = new BenchmarkDataset("benchmark-v1", RepositoryActivityCollector.VERSION, start, end, Instant.now(),
                RepositoryActivityCollector.SCOPE, repositories);
        BenchmarkCatalog.validate(dataset);
        Path output = Path.of(args[2]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        // Preserve old content-addressed reference snapshots; never silently replace one.
        Files.writeString(output, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataset).replace("\r\n", "\n") + "\n",
                java.nio.file.StandardOpenOption.CREATE_NEW);
        System.out.println("Wrote public aggregate benchmark: " + output);
    }
}
