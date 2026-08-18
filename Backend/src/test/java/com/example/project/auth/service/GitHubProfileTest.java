package com.example.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class GitHubProfileTest {

    @Test
    void mapsGitHubAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("login", "octocat");
        attributes.put("email", "octocat@example.com");

        GitHubProfile profile = GitHubProfile.from(attributes);

        assertThat(profile.id()).isEqualTo(12345L);
        assertThat(profile.username()).isEqualTo("octocat");
        assertThat(profile.email()).isEqualTo("octocat@example.com");
    }

    @Test
    void allowsPrivateEmail() {
        GitHubProfile profile = GitHubProfile.from(Map.of(
                "id", "12345",
                "login", "octocat"));

        assertThat(profile.email()).isNull();
    }

    @Test
    void rejectsMissingStableGitHubId() {
        assertThatThrownBy(() -> GitHubProfile.from(Map.of("login", "octocat")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("GitHub user id is missing");
    }
}
