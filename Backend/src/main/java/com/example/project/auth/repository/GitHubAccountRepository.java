package com.example.project.auth.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.project.auth.domain.GitHubAccount;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubAccountRepository extends JpaRepository<GitHubAccount, UUID> {

    Optional<GitHubAccount> findByGithubUserId(long githubUserId);

    Optional<GitHubAccount> findByUserId(UUID userId);
}
