package com.example.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.domain.User;
import com.example.project.auth.repository.GitHubAccountRepository;
import com.example.project.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class GitHubAccountConnectionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GitHubAccountRepository accountRepository;

    private GitHubAccountConnectionService connectionService;

    @BeforeEach
    void setUp() {
        connectionService = new GitHubAccountConnectionService(
                userRepository,
                accountRepository);
    }

    @Test
    void createsUserAndGitHubAccountOnFirstLogin() {
        when(accountRepository.findByGithubUserId(1001L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(GitHubAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GitHubPrincipal principal = connectionService.connect(
                new GitHubProfile(1001L, "octocat", "octocat@example.com"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        ArgumentCaptor<GitHubAccount> accountCaptor = ArgumentCaptor.forClass(GitHubAccount.class);
        verify(accountRepository).save(accountCaptor.capture());
        GitHubAccount savedAccount = accountCaptor.getValue();

        assertThat(principal.getUserId()).isNotNull();
        assertThat(principal.getGithubUserId()).isEqualTo(1001L);
        assertThat(savedAccount.getEncryptedAccessToken()).isNull();
    }

    @Test
    void reconnectsExistingAccountWithoutCreatingUser() {
        User user = User.create();
        GitHubAccount account = GitHubAccount.connect(
                user,
                1001L,
                "old-name",
                null,
                Instant.parse("2026-01-01T00:00:00Z"));
        account.revoke(Instant.parse("2026-02-01T00:00:00Z"));

        when(accountRepository.findByGithubUserId(1001L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        GitHubPrincipal principal = connectionService.connect(
                new GitHubProfile(1001L, "new-name", "new@example.com"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        verify(userRepository, never()).save(any());
        assertThat(principal.getGithubUsername()).isEqualTo("new-name");
        assertThat(account.getRevokedAt()).isNull();
    }
}
