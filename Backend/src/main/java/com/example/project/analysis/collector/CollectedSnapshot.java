package com.example.project.analysis.collector;

import java.time.Instant;
import java.util.List;

public record CollectedSnapshot(
        Instant collectedAt,
        String sourceMetadata,
        String snapshotHash,
        List<CollectedActivity> activities) {
}
