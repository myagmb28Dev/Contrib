package com.example.project.common.exception;

public class GitHubReauthorizationRequiredException extends RuntimeException {

    public GitHubReauthorizationRequiredException() {
        super("GitHub authorization has expired. Sign in with GitHub again.");
    }

    public GitHubReauthorizationRequiredException(Throwable cause) {
        super("GitHub authorization has expired. Sign in with GitHub again.", cause);
    }
}
