package com.example.project.github.client;

import java.time.Instant;

public class GitHubApiException extends RuntimeException {
    private final int statusCode;
    private final Instant retryAt;
    private final boolean rateLimited;

    public GitHubApiException(int statusCode, String message) {
        this(statusCode, message, null, false);
    }

    public GitHubApiException(int statusCode, String message, Instant retryAt) {
        this(statusCode, message, retryAt, true);
    }

    private GitHubApiException(int statusCode, String message, Instant retryAt, boolean rateLimited) {
        super(message);
        this.statusCode = statusCode;
        this.retryAt = retryAt;
        this.rateLimited = rateLimited;
    }

    public int getStatusCode() { return statusCode; }
    public Instant getRetryAt() { return retryAt; }
    public boolean isRateLimited() { return rateLimited; }
}
