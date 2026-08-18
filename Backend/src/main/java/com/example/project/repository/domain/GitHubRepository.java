package com.example.project.repository.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.auth.domain.User;
import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "github_repository",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_github_repository_user_repo",
                columnNames = {"user_id", "github_repository_id"}))
public class GitHubRepository extends BaseTimeEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repository_id", nullable = false)
    private long githubRepositoryId;

    @Column(name = "owner_github_id", nullable = false)
    private long ownerGithubId;

    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false, length = 512)
    private String fullName;

    @Column(name = "html_url", nullable = false, length = 1024)
    private String htmlUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepositoryVisibility visibility;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    private String language;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    protected GitHubRepository() {
    }

    private GitHubRepository(UUID id, User user, long githubRepositoryId) {
        this.id = id;
        this.user = user;
        this.githubRepositoryId = githubRepositoryId;
    }

    public static GitHubRepository create(User user, long githubRepositoryId) {
        return new GitHubRepository(UUID.randomUUID(), user, githubRepositoryId);
    }

    public void synchronize(
            long ownerGithubId,
            String ownerLogin,
            String name,
            String fullName,
            String htmlUrl,
            String defaultBranch,
            String language,
            boolean archived,
            Instant synchronizedAt) {
        this.ownerGithubId = ownerGithubId;
        this.ownerLogin = ownerLogin;
        this.name = name;
        this.fullName = fullName;
        this.htmlUrl = htmlUrl;
        this.visibility = RepositoryVisibility.PUBLIC;
        this.defaultBranch = defaultBranch;
        this.language = language;
        this.archived = archived;
        this.lastSyncedAt = synchronizedAt;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public long getGithubRepositoryId() { return githubRepositoryId; }
    public long getOwnerGithubId() { return ownerGithubId; }
    public String getOwnerLogin() { return ownerLogin; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getHtmlUrl() { return htmlUrl; }
    public RepositoryVisibility getVisibility() { return visibility; }
    public String getDefaultBranch() { return defaultBranch; }
    public String getLanguage() { return language; }
    public boolean isArchived() { return archived; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
}
