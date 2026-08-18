package com.example.project.github.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestDto(
        long id,
        int number,
        String title,
        String state,
        GitHubUserDto user,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("merged_at") Instant mergedAt) {
}
