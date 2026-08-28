package com.example.project.repository.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.project.repository.domain.GitHubRepository;

public record RepositoryResponse(
        UUID id,
        long githubRepositoryId,
        String ownerLogin,
        String name,
        String fullName,
        String url,
        String visibility,
        String defaultBranch,
        String language,
        boolean archived,
        Instant lastSyncedAt,
        Instant createdAt) {

    public static RepositoryResponse from(GitHubRepository repository) {
        return new RepositoryResponse(repository.getId(), repository.getGithubRepositoryId(),
                repository.getOwnerLogin(), repository.getName(), repository.getFullName(),
                repository.getHtmlUrl(), repository.getVisibility().name(), repository.getDefaultBranch(),
                repository.getLanguage(), repository.isArchived(), repository.getLastSyncedAt(),
                repository.getCreatedAt());
    }
}
