package com.example.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.example.project.common.exception.GitHubReauthorizationRequiredException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@ExtendWith(MockitoExtension.class)
class GitHubAccessTokenServiceTest {

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Test
    void returnsAccessTokenProvidedByAuthorizedClientManager() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(authorizedClient("fresh-access-token"));

        String token = new GitHubAccessTokenService(authorizedClientManager)
                .getValidAccessToken(UUID.randomUUID());

        assertThat(token).isEqualTo("fresh-access-token");
    }

    @Test
    void requiresReauthorizationWhenTokenCannotBeLoadedOrRefreshed() {
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> new GitHubAccessTokenService(authorizedClientManager)
                .getValidAccessToken(UUID.randomUUID()))
                .isInstanceOf(GitHubReauthorizationRequiredException.class);
    }

    private OAuth2AuthorizedClient authorizedClient(String tokenValue) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("github")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of());
        return new OAuth2AuthorizedClient(registration, UUID.randomUUID().toString(), accessToken);
    }
}
