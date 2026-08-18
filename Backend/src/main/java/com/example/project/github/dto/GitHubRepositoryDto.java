package com.example.project.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryDto(
        long id,
        GitHubUserDto owner,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("private") boolean privateRepository,
        @JsonProperty("default_branch") String defaultBranch,
        String language,
        boolean archived) {
}
