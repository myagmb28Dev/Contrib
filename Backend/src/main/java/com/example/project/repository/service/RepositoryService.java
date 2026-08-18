package com.example.project.repository.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.project.auth.domain.User;
import com.example.project.auth.repository.UserRepository;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.example.project.common.exception.ResourceNotFoundException;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.repository.domain.GitHubRepository;
import com.example.project.repository.dto.RepositoryResponse;
import com.example.project.repository.repository.GitHubRepositoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryService {

    private final GitHubApiClient githubApiClient;
    private final GitHubAccessTokenService accessTokenService;
    private final UserRepository userRepository;
    private final GitHubRepositoryRepository repository;

    public RepositoryService(GitHubApiClient githubApiClient, GitHubAccessTokenService accessTokenService,
            UserRepository userRepository, GitHubRepositoryRepository repository) {
        this.githubApiClient = githubApiClient;
        this.accessTokenService = accessTokenService;
        this.userRepository = userRepository;
        this.repository = repository;
    }

    @Transactional
    public List<RepositoryResponse> synchronize(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String token = accessTokenService.getValidAccessToken(userId);
        Instant now = Instant.now();
        for (GitHubRepositoryDto source : githubApiClient.getPublicRepositories(token)) {
            if (source.privateRepository()) {
                continue;
            }
            GitHubRepository target = repository.findByUserIdAndGithubRepositoryId(userId, source.id())
                    .orElseGet(() -> GitHubRepository.create(user, source.id()));
            target.synchronize(source.owner().id(), source.owner().login(), source.name(), source.fullName(),
                    source.htmlUrl(), source.defaultBranch(), source.language(), source.archived(), now);
            repository.save(target);
        }
        return list(userId);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> list(UUID userId) {
        return repository.findAllByUserIdOrderByFullNameAsc(userId).stream().map(RepositoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RepositoryResponse get(UUID userId, UUID repositoryId) {
        return RepositoryResponse.from(getOwnedRepository(userId, repositoryId));
    }

    @Transactional(readOnly = true)
    public GitHubRepository getOwnedRepository(UUID userId, UUID repositoryId) {
        return repository.findByIdAndUserId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
    }
}
