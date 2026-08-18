package com.example.project.auth.controller;

import java.util.UUID;

import com.example.project.auth.service.GitHubPrincipal;

public record AuthMeResponse(
        UUID userId,
        long githubUserId,
        String githubUsername,
        String email) {

    public static AuthMeResponse from(GitHubPrincipal principal) {
        return new AuthMeResponse(
                principal.getUserId(),
                principal.getGithubUserId(),
                principal.getGithubUsername(),
                principal.getEmail());
    }
}
