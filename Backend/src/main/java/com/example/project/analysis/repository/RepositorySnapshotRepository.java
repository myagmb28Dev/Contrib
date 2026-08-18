package com.example.project.analysis.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.project.analysis.domain.RepositorySnapshot;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorySnapshotRepository extends JpaRepository<RepositorySnapshot, UUID> {
    Optional<RepositorySnapshot> findByAnalysisJobId(UUID analysisJobId);
}
