package com.example.project.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubBranchDto(
        String name,
        @JsonProperty("protected") boolean protectedBranch) {
}
