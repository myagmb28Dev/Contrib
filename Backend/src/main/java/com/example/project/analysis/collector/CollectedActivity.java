package com.example.project.analysis.collector;

import java.time.Instant;

import com.example.project.analysis.domain.ActivityType;

public record CollectedActivity(
        String externalId,
        ActivityType type,
        long authorGithubId,
        Instant occurredAt,
        String title,
        String state,
        int additions,
        int deletions,
        int changedFiles,
        String rawPayload) {
}
