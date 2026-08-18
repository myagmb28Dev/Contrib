package com.example.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.domain.User;
import com.example.project.auth.repository.GitHubAccountRepository;
import com.example.project.common.security.AccessTokenCipher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

@ExtendWith(MockitoExtension.class)
class DatabaseOAuth2AuthorizedClientServiceTest {

    @Mock
    private ClientRegistrationRepository registrationRepository;

    @Mock
    private GitHubAccountRepository accountRepository;

    private AccessTokenCipher tokenCipher;
    private DatabaseOAuth2AuthorizedClientService authorizedClientService;
    private ClientRegistration registration;
    private User user;
    private GitHubAccount account;

    @BeforeEach
    void setUp() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        tokenCipher = new AccessTokenCipher(key);
        authorizedClientService = new DatabaseOAuth2AuthorizedClientService(
                registrationRepository,
                accountRepository,
                tokenCipher,
                Duration.ofSeconds(15_897_600));

        registration = ClientRegistration.withRegistrationId("github")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();

        user = User.create();
        account = GitHubAccount.connect(
                user,
                1001L,
                "octocat",
                "octocat@example.com",
                Instant.parse("2026-08-18T00:00:00Z"));

        when(accountRepository.findByUserId(user.getId())).thenReturn(Optional.of(account));
    }

    @Test
    void encryptsAndPersistsAccessAndRefreshTokens() {
        authorizedClientService.saveAuthorizedClient(
                authorizedClient("access-token-v1", "refresh-token-v1"),
                authentication());

        assertThat(account.getEncryptedAccessToken()).doesNotContain("access-token-v1");
        assertThat(account.getEncryptedRefreshToken()).doesNotContain("refresh-token-v1");
        assertThat(tokenCipher.decrypt(account.getEncryptedAccessToken())).isEqualTo("access-token-v1");
        assertThat(tokenCipher.decrypt(account.getEncryptedRefreshToken())).isEqualTo("refresh-token-v1");
        assertThat(account.getTokenExpiresAt()).isEqualTo("2026-08-18T08:00:00Z");
        assertThat(account.getRefreshTokenExpiresAt()).isEqualTo("2027-02-18T00:00:00Z");
    }

    @Test
    void reconstructsAuthorizedClientFromEncryptedDatabaseTokens() {
        when(registrationRepository.findByRegistrationId("github")).thenReturn(registration);
        authorizedClientService.saveAuthorizedClient(
                authorizedClient("access-token-v2", "refresh-token-v2"),
                authentication());

        OAuth2AuthorizedClient loaded = authorizedClientService.loadAuthorizedClient(
                "github",
                user.getId().toString());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getAccessToken().getTokenValue()).isEqualTo("access-token-v2");
        assertThat(loaded.getRefreshToken()).isNotNull();
        assertThat(loaded.getRefreshToken().getTokenValue()).isEqualTo("refresh-token-v2");
    }

    @Test
    void disconnectsAndClearsStoredTokens() {
        authorizedClientService.saveAuthorizedClient(
                authorizedClient("access-token", "refresh-token"),
                authentication());

        authorizedClientService.removeAuthorizedClient("github", user.getId().toString());

        assertThat(account.getEncryptedAccessToken()).isNull();
        assertThat(account.getEncryptedRefreshToken()).isNull();
        assertThat(account.getRevokedAt()).isNotNull();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                user.getId().toString(),
                "N/A",
                List.of());
    }

    private OAuth2AuthorizedClient authorizedClient(String accessTokenValue, String refreshTokenValue) {
        Instant issuedAt = Instant.parse("2026-08-18T00:00:00Z");
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                issuedAt,
                Instant.parse("2026-08-18T08:00:00Z"),
                registration.getScopes());
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                refreshTokenValue,
                issuedAt);
        return new OAuth2AuthorizedClient(
                registration,
                user.getId().toString(),
                accessToken,
                refreshToken);
    }
}
