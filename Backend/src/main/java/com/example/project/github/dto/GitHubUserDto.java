package com.example.project.github.dto;

public record GitHubUserDto(long id, String login, String type) {
    public GitHubUserDto(long id, String login) { this(id, login, null); }

    public boolean isBot() {
        return "Bot".equalsIgnoreCase(type)
                || login != null && login.toLowerCase(java.util.Locale.ROOT).endsWith("[bot]");
    }
}
