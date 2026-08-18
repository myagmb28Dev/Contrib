package com.example.project.analysis.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.auth.domain.User;
import com.example.project.common.auditing.BaseTimeEntity;
import com.example.project.repository.domain.GitHubRepository;

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
@Table(name = "analysis_job")
public class AnalysisJob extends BaseTimeEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitHubRepository repository;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "collector_version", nullable = false)
    private String collectorVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisJobStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AnalysisJob() {
    }

    private AnalysisJob(UUID id, User user, GitHubRepository repository, Instant periodStart,
            Instant periodEnd, String collectorVersion) {
        this.id = id;
        this.user = user;
        this.repository = repository;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.collectorVersion = collectorVersion;
        this.status = AnalysisJobStatus.PENDING;
        this.progress = 0;
    }

    public static AnalysisJob create(User user, GitHubRepository repository, Instant periodStart,
            Instant periodEnd, String collectorVersion) {
        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }
        return new AnalysisJob(UUID.randomUUID(), user, repository, periodStart, periodEnd, collectorVersion);
    }

    public void startCollection() { status = AnalysisJobStatus.COLLECTING; progress = 10; startedAt = Instant.now(); }
    public void startAnalysis() { status = AnalysisJobStatus.ANALYZING; progress = 60; }
    public void startAiProcessing() { status = AnalysisJobStatus.AI_PROCESSING; progress = 85; }
    public void complete() { status = AnalysisJobStatus.COMPLETED; progress = 100; completedAt = Instant.now(); }
    public void fail(String code, String message) {
        status = AnalysisJobStatus.FAILED;
        errorCode = code;
        errorMessage = message == null ? null : message.substring(0, Math.min(message.length(), 2000));
        completedAt = Instant.now();
    }
    public void retry() {
        if (status != AnalysisJobStatus.FAILED) {
            throw new IllegalStateException("Only a failed analysis job can be retried");
        }
        status = AnalysisJobStatus.PENDING;
        progress = 0;
        errorCode = null;
        errorMessage = null;
        startedAt = null;
        completedAt = null;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public GitHubRepository getRepository() { return repository; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public String getCollectorVersion() { return collectorVersion; }
    public AnalysisJobStatus getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
