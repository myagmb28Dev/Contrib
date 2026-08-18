package com.example.project.auth.service;

import java.time.Instant;
import java.util.Collection;

import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.domain.User;
import com.example.project.auth.repository.GitHubAccountRepository;
import com.example.project.auth.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubAccountConnectionService {

    private final UserRepository userRepository;
    private final GitHubAccountRepository accountRepository;
    public GitHubAccountConnectionService(
            UserRepository userRepository,
            GitHubAccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public GitHubPrincipal connect(
            GitHubProfile profile,
            Collection<? extends GrantedAuthority> authorities) {
        Instant connectedAt = Instant.now();

        GitHubAccount account = accountRepository.findByGithubUserId(profile.id())
                .map(existing -> reconnect(
                        existing,
                        profile,
                        connectedAt))
                .orElseGet(() -> createAccount(
                        profile,
                        connectedAt));

        return new GitHubPrincipal(
                account.getUser().getId(),
                account.getGithubUserId(),
                account.getGithubUsername(),
                account.getEmail(),
                authorities);
    }

    private GitHubAccount createAccount(
            GitHubProfile profile,
            Instant connectedAt) {
        User user = userRepository.save(User.create());
        GitHubAccount account = GitHubAccount.connect(
                user,
                profile.id(),
                profile.username(),
                profile.email(),
                connectedAt);
        return accountRepository.save(account);
    }

    private GitHubAccount reconnect(
            GitHubAccount account,
            GitHubProfile profile,
            Instant connectedAt) {
        account.reconnect(
                profile.username(),
                profile.email(),
                connectedAt);
        return accountRepository.save(account);
    }
}
