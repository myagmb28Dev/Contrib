package com.example.project.repository.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.project.repository.domain.GitHubRepository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, UUID> {
    List<GitHubRepository> findAllByUserIdOrderByFullNameAsc(UUID userId);
    Optional<GitHubRepository> findByIdAndUserId(UUID id, UUID userId);
    Optional<GitHubRepository> findByUserIdAndGithubRepositoryId(UUID userId, long githubRepositoryId);
}
