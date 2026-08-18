package com.example.project.analysis.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.project.analysis.domain.ContributionAnalysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionAnalysisRepository extends JpaRepository<ContributionAnalysis, UUID> {
    Optional<ContributionAnalysis> findByIdAndSnapshotAnalysisJobUserId(UUID id, UUID userId);
    Optional<ContributionAnalysis> findBySnapshotAnalysisJobId(UUID analysisJobId);
    Optional<ContributionAnalysis> findBySnapshotId(UUID snapshotId);
    List<ContributionAnalysis> findAllBySnapshotRepositoryIdAndSnapshotAnalysisJobUserIdOrderByCreatedAtDesc(
            UUID repositoryId, UUID userId);
    List<ContributionAnalysis> findAllBySnapshotAnalysisJobUserIdOrderByCreatedAtDesc(UUID userId);
}
