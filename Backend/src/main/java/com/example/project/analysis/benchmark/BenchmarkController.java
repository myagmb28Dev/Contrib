package com.example.project.analysis.benchmark;

import java.time.Instant;
import java.util.List;
import com.example.project.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/benchmarks")
public class BenchmarkController {
    private final BenchmarkCatalog catalog;
    public BenchmarkController(BenchmarkCatalog catalog) { this.catalog = catalog; }
    public record Summary(String id, Instant periodStart, Instant periodEnd, Instant collectedAt,
            int repositoryCount, List<String> languages) {}
    @GetMapping
    public List<Summary> list() {
        return catalog.entries().stream().map(e -> new Summary(e.id(), e.dataset().periodStart(), e.dataset().periodEnd(),
                e.dataset().collectedAt(), e.dataset().repositories().size(), e.dataset().repositories().stream()
                        .map(BenchmarkDataset.Repository::language).distinct().sorted().toList())).toList();
    }
    @GetMapping(value = "/{id}", produces = "application/json")
    public org.springframework.http.ResponseEntity<String> get(@PathVariable String id) {
        var entry = catalog.entries().stream().filter(e -> e.id().equals(id))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Benchmark snapshot not found"));
        return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(entry.json());
    }
}
