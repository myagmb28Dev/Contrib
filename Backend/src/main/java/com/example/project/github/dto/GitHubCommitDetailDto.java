package com.example.project.github.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitDetailDto(String sha, Stats stats, List<FileChange> files) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(int additions, int deletions, int total) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileChange(@JsonProperty("filename") String filename) {}
}
