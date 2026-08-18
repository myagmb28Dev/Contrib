package com.example.project.github.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitDto(String sha, GitHubUserDto author, CommitData commit, List<Parent> parents) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitData(CommitAuthor author, String message) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitAuthor(Instant date) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parent(String sha) {}
}
