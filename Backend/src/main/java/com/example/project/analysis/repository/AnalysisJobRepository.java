package com.example.project.analysis.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.project.analysis.domain.AnalysisJob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
    Optional<AnalysisJob> findByIdAndUserId(UUID id, UUID userId);
    @EntityGraph(attributePaths = {"user", "repository"})
    Optional<AnalysisJob> findDetailedById(UUID id);
    Optional<AnalysisJob> findByUserIdAndRepositoryIdAndPeriodStartAndPeriodEndAndCollectorVersionAndTargetBranch(
            UUID userId, UUID repositoryId, Instant periodStart, Instant periodEnd, String collectorVersion, String targetBranch);
    Optional<AnalysisJob> findByUserIdAndRepositoryIdAndPeriodStartAndPeriodEndAndCollectorVersion(
            UUID userId, UUID repositoryId, Instant periodStart, Instant periodEnd, String collectorVersion);
    List<AnalysisJob> findAllByRepositoryIdAndUserIdOrderByCreatedAtDesc(UUID repositoryId, UUID userId);

    @Query("""
            SELECT job.id FROM AnalysisJob job
            WHERE (
                (job.status = com.example.project.analysis.domain.AnalysisJobStatus.PENDING
                    AND (job.nextAttemptAt IS NULL OR job.nextAttemptAt <= :now))
                OR (job.status IN (
                        com.example.project.analysis.domain.AnalysisJobStatus.COLLECTING,
                        com.example.project.analysis.domain.AnalysisJobStatus.ANALYZING,
                        com.example.project.analysis.domain.AnalysisJobStatus.AI_PROCESSING)
                    AND (job.leaseExpiresAt IS NULL OR job.leaseExpiresAt <= :now))
            )
            ORDER BY job.createdAt
            """)
    List<UUID> findClaimableIds(@Param("now") Instant now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE analysis_job
            SET status = 'COLLECTING', progress = 10,
                started_at = COALESCE(started_at, :now),
                attempt_count = attempt_count + 1,
                next_attempt_at = NULL,
                lease_expires_at = :leaseUntil,
                updated_at = :now
            WHERE id = :id AND (
                (status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= :now))
                OR (status IN ('COLLECTING', 'ANALYZING', 'AI_PROCESSING')
                    AND (lease_expires_at IS NULL OR lease_expires_at <= :now))
            )
            """, nativeQuery = true)
    int claim(@Param("id") UUID id, @Param("now") Instant now, @Param("leaseUntil") Instant leaseUntil);
}
