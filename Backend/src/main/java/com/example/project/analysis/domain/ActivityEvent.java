package com.example.project.analysis.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_event")
public class ActivityEvent extends BaseTimeEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private RepositorySnapshot snapshot;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(name = "author_github_id", nullable = false)
    private long authorGithubId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT")
    private String title;

    private String state;

    @Column(nullable = false)
    private int additions;

    @Column(nullable = false)
    private int deletions;

    @Column(name = "changed_files", nullable = false)
    private int changedFiles;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    protected ActivityEvent() {
    }

    public static ActivityEvent create(RepositorySnapshot snapshot, String externalId, ActivityType type,
            long authorGithubId, Instant occurredAt, String title, String state,
            int additions, int deletions, int changedFiles, String rawPayload) {
        ActivityEvent event = new ActivityEvent();
        event.id = UUID.randomUUID();
        event.snapshot = snapshot;
        event.externalId = externalId;
        event.type = type;
        event.authorGithubId = authorGithubId;
        event.occurredAt = occurredAt;
        event.title = title;
        event.state = state;
        event.additions = additions;
        event.deletions = deletions;
        event.changedFiles = changedFiles;
        event.rawPayload = rawPayload;
        return event;
    }

    public UUID getId() { return id; }
    public RepositorySnapshot getSnapshot() { return snapshot; }
    public String getExternalId() { return externalId; }
    public ActivityType getType() { return type; }
    public long getAuthorGithubId() { return authorGithubId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getTitle() { return title; }
    public String getState() { return state; }
    public int getAdditions() { return additions; }
    public int getDeletions() { return deletions; }
    public int getChangedFiles() { return changedFiles; }
    public String getRawPayload() { return rawPayload; }
}
