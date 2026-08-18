package com.example.project.auth.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "github_account")
public class GitHubAccount extends BaseTimeEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "github_user_id", nullable = false, unique = true)
    private long githubUserId;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(length = 320)
    private String email;

    @Column(name = "encrypted_access_token", columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "access_token_issued_at")
    private Instant accessTokenIssuedAt;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "refresh_token_issued_at")
    private Instant refreshTokenIssuedAt;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected GitHubAccount() {
    }

    private GitHubAccount(
            UUID id,
            User user,
            long githubUserId,
            String githubUsername,
            String email,
            Instant connectedAt) {
        this.id = id;
        this.user = user;
        this.githubUserId = githubUserId;
        this.githubUsername = githubUsername;
        this.email = email;
        this.connectedAt = connectedAt;
    }

    public static GitHubAccount connect(
            User user,
            long githubUserId,
            String githubUsername,
            String email,
            Instant connectedAt) {
        return new GitHubAccount(
                UUID.randomUUID(),
                user,
                githubUserId,
                githubUsername,
                email,
                connectedAt);
    }

    public void reconnect(
            String githubUsername,
            String email,
            Instant connectedAt) {
        this.githubUsername = githubUsername;
        this.email = email;
        this.connectedAt = connectedAt;
        this.revokedAt = null;
    }

    public void updateTokens(
            String encryptedAccessToken,
            Instant accessTokenIssuedAt,
            Instant accessTokenExpiresAt,
            String encryptedRefreshToken,
            Instant refreshTokenIssuedAt,
            Instant refreshTokenExpiresAt) {
        this.encryptedAccessToken = encryptedAccessToken;
        this.accessTokenIssuedAt = accessTokenIssuedAt;
        this.tokenExpiresAt = accessTokenExpiresAt;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.refreshTokenIssuedAt = refreshTokenIssuedAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.revokedAt = null;
    }

    public void disconnect(Instant revokedAt) {
        this.encryptedAccessToken = null;
        this.accessTokenIssuedAt = null;
        this.tokenExpiresAt = null;
        this.encryptedRefreshToken = null;
        this.refreshTokenIssuedAt = null;
        this.refreshTokenExpiresAt = null;
        this.revokedAt = revokedAt;
    }

    public void revoke(Instant revokedAt) {
        disconnect(revokedAt);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public long getGithubUserId() {
        return githubUserId;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public String getEmail() {
        return email;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public Instant getAccessTokenIssuedAt() {
        return accessTokenIssuedAt;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public Instant getRefreshTokenIssuedAt() {
        return refreshTokenIssuedAt;
    }

    public Instant getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
