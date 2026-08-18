package com.example.project.auth.service;

import java.util.List;
import java.util.UUID;

import com.example.project.common.exception.GitHubReauthorizationRequiredException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;

@Service
public class GitHubAccessTokenService {

    private static final String REGISTRATION_ID = "github";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public GitHubAccessTokenService(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getValidAccessToken(UUID userId) {
        Authentication principal = UsernamePasswordAuthenticationToken.authenticated(
                userId.toString(),
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(principal)
                .build();

        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
            if (authorizedClient == null) {
                throw new GitHubReauthorizationRequiredException();
            }
            return authorizedClient.getAccessToken().getTokenValue();
        } catch (OAuth2AuthorizationException exception) {
            throw new GitHubReauthorizationRequiredException(exception);
        }
    }
}
