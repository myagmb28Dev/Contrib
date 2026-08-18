package com.example.project.auth.service;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.repository.GitHubAccountRepository;
import com.example.project.common.security.AccessTokenCipher;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final GitHubAccountRepository accountRepository;
    private final AccessTokenCipher tokenCipher;
    private final Duration refreshTokenTtl;

    public DatabaseOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            GitHubAccountRepository accountRepository,
            AccessTokenCipher tokenCipher,
            @Value("${app.github.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.accountRepository = accountRepository;
        this.tokenCipher = tokenCipher;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId,
            String principalName) {
        ClientRegistration registration = clientRegistrationRepository
                .findByRegistrationId(clientRegistrationId);
        UUID userId = parseUserId(principalName);
        if (registration == null || userId == null) {
            return null;
        }

        GitHubAccount account = accountRepository.findByUserId(userId).orElse(null);
        if (account == null
                || account.getEncryptedAccessToken() == null
                || account.getAccessTokenIssuedAt() == null) {
            return null;
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenCipher.decrypt(account.getEncryptedAccessToken()),
                account.getAccessTokenIssuedAt(),
                account.getTokenExpiresAt(),
                registration.getScopes());

        OAuth2RefreshToken refreshToken = null;
        if (account.getEncryptedRefreshToken() != null) {
            refreshToken = new OAuth2RefreshToken(
                    tokenCipher.decrypt(account.getEncryptedRefreshToken()),
                    account.getRefreshTokenIssuedAt(),
                    account.getRefreshTokenExpiresAt());
        }

        return (T) new OAuth2AuthorizedClient(
                registration,
                principalName,
                accessToken,
                refreshToken);
    }

    @Override
    @Transactional
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient,
            Authentication principal) {
        UUID userId = requireUserId(principal.getName());
        GitHubAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "GitHub account does not exist for authenticated user"));

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
        Instant refreshTokenExpiresAt = refreshToken == null ? null : refreshToken.getExpiresAt();
        if (refreshToken != null
                && refreshTokenExpiresAt == null
                && refreshToken.getIssuedAt() != null) {
            refreshTokenExpiresAt = refreshToken.getIssuedAt().plus(refreshTokenTtl);
        }

        account.updateTokens(
                tokenCipher.encrypt(accessToken.getTokenValue()),
                accessToken.getIssuedAt(),
                accessToken.getExpiresAt(),
                refreshToken == null ? null : tokenCipher.encrypt(refreshToken.getTokenValue()),
                refreshToken == null ? null : refreshToken.getIssuedAt(),
                refreshTokenExpiresAt);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        UUID userId = parseUserId(principalName);
        if (userId == null) {
            return;
        }

        accountRepository.findByUserId(userId).ifPresent(account -> {
            account.disconnect(Instant.now());
            accountRepository.save(account);
        });
    }

    private static UUID requireUserId(String principalName) {
        UUID userId = parseUserId(principalName);
        if (userId == null) {
            throw new IllegalArgumentException("OAuth principal name must be a user UUID");
        }
        return userId;
    }

    private static UUID parseUserId(String principalName) {
        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
