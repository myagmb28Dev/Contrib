package com.example.project.analysis.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.project.analysis.domain.AnalysisJob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
    Optional<AnalysisJob> findByIdAndUserId(UUID id, UUID userId);
    @EntityGraph(attributePaths = {"user", "repository"})
    Optional<AnalysisJob> findDetailedById(UUID id);
    Optional<AnalysisJob> findByUserIdAndRepositoryIdAndPeriodStartAndPeriodEndAndCollectorVersion(
            UUID userId, UUID repositoryId, Instant periodStart, Instant periodEnd, String collectorVersion);
    List<AnalysisJob> findAllByRepositoryIdAndUserIdOrderByCreatedAtDesc(UUID repositoryId, UUID userId);
}
