package com.example.project.analysis.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.common.auditing.BaseTimeEntity;
import com.example.project.repository.domain.GitHubRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "repository_snapshot")
public class RepositorySnapshot extends BaseTimeEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_job_id", nullable = false, unique = true)
    private AnalysisJob analysisJob;

    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitHubRepository repository;

    @Column(name = "subject_github_id", nullable = false)
    private long subjectGithubId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "collector_version", nullable = false)
    private String collectorVersion;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "source_metadata", nullable = false, columnDefinition = "TEXT")
    private String sourceMetadata;

    @Column(name = "snapshot_hash", nullable = false, length = 66)
    private String snapshotHash;

    protected RepositorySnapshot() {
    }

    public static RepositorySnapshot create(AnalysisJob job, long subjectGithubId,
            Instant collectedAt, String sourceMetadata, String snapshotHash) {
        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.id = UUID.randomUUID();
        snapshot.analysisJob = job;
        snapshot.repository = job.getRepository();
        snapshot.subjectGithubId = subjectGithubId;
        snapshot.periodStart = job.getPeriodStart();
        snapshot.periodEnd = job.getPeriodEnd();
        snapshot.collectorVersion = job.getCollectorVersion();
        snapshot.collectedAt = collectedAt;
        snapshot.sourceMetadata = sourceMetadata;
        snapshot.snapshotHash = snapshotHash;
        return snapshot;
    }

    public UUID getId() { return id; }
    public AnalysisJob getAnalysisJob() { return analysisJob; }
    public GitHubRepository getRepository() { return repository; }
    public long getSubjectGithubId() { return subjectGithubId; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public String getCollectorVersion() { return collectorVersion; }
    public Instant getCollectedAt() { return collectedAt; }
    public String getSourceMetadata() { return sourceMetadata; }
    public String getSnapshotHash() { return snapshotHash; }
}
