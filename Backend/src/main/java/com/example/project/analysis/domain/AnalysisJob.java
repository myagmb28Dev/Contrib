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

    @Column(name = "target_branch", nullable = false)
    private String targetBranch;

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

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    protected AnalysisJob() {
    }

    private AnalysisJob(UUID id, User user, GitHubRepository repository, Instant periodStart,
            Instant periodEnd, String collectorVersion, String targetBranch) {
        this.id = id;
        this.user = user;
        this.repository = repository;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.collectorVersion = collectorVersion;
        this.targetBranch = (targetBranch != null && !targetBranch.isBlank())
                ? targetBranch.trim()
                : (repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main");
        this.status = AnalysisJobStatus.PENDING;
        this.progress = 0;
        this.attemptCount = 0;
        this.nextAttemptAt = Instant.now();
    }

    public static AnalysisJob create(User user, GitHubRepository repository, Instant periodStart,
            Instant periodEnd, String collectorVersion, String targetBranch) {
        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException("periodStart must be before periodEnd");
        }
        return new AnalysisJob(UUID.randomUUID(), user, repository, periodStart, periodEnd, collectorVersion, targetBranch);
    }

    public static AnalysisJob create(User user, GitHubRepository repository, Instant periodStart,
            Instant periodEnd, String collectorVersion) {
        return create(user, repository, periodStart, periodEnd, collectorVersion, repository.getDefaultBranch());
    }

    public void startCollection(Instant leaseExpiresAt) {
        status = AnalysisJobStatus.COLLECTING;
        progress = 10;
        startedAt = startedAt == null ? Instant.now() : startedAt;
        this.leaseExpiresAt = leaseExpiresAt;
    }
    public void startAnalysis(Instant leaseExpiresAt) {
        status = AnalysisJobStatus.ANALYZING;
        progress = 60;
        this.leaseExpiresAt = leaseExpiresAt;
    }
    public void startAiProcessing(Instant leaseExpiresAt) {
        status = AnalysisJobStatus.AI_PROCESSING;
        progress = 85;
        this.leaseExpiresAt = leaseExpiresAt;
    }
    public void complete() {
        status = AnalysisJobStatus.COMPLETED;
        progress = 100;
        completedAt = Instant.now();
        leaseExpiresAt = null;
        nextAttemptAt = null;
    }
    public void fail(String code, String message) {
        status = AnalysisJobStatus.FAILED;
        errorCode = code;
        errorMessage = message == null ? null : message.substring(0, Math.min(message.length(), 2000));
        completedAt = Instant.now();
        leaseExpiresAt = null;
        nextAttemptAt = null;
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
        leaseExpiresAt = null;
        nextAttemptAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public GitHubRepository getRepository() { return repository; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public String getCollectorVersion() { return collectorVersion; }
    public String getTargetBranch() { return targetBranch; }
    public AnalysisJobStatus getStatus() { return status; }
    public int getProgress() { return progress; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
}
