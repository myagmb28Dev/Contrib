package com.example.project.github.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubReviewDto(
        long id,
        String state,
        GitHubUserDto user,
        @JsonProperty("submitted_at") Instant submittedAt) {
}
