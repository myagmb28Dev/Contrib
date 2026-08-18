package com.example.project.auth.service;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class GitHubPrincipal implements OAuth2User, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final long githubUserId;
    private final String githubUsername;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;

    public GitHubPrincipal(
            UUID userId,
            long githubUserId,
            String githubUsername,
            String email,
            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.githubUserId = githubUserId;
        this.githubUsername = githubUsername;
        this.email = email;
        this.authorities = Collections.unmodifiableCollection(authorities);

        Map<String, Object> safeAttributes = new LinkedHashMap<>();
        safeAttributes.put("id", githubUserId);
        safeAttributes.put("login", githubUsername);
        if (email != null) {
            safeAttributes.put("email", email);
        }
        this.attributes = Collections.unmodifiableMap(safeAttributes);
    }

    public UUID getUserId() {
        return userId;
    }

    public long getGithubUserId() {
        return githubUserId;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
