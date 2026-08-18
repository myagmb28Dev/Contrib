package com.example.project.github.dto;

public record GitHubUserDto(long id, String login) {
    public boolean isBot() {
        return login != null && (login.endsWith("[bot]") || login.toLowerCase().contains("bot"));
    }
}
