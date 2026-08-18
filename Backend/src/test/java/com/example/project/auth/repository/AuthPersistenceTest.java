package com.example.project.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.domain.User;
import com.example.project.common.auditing.JpaAuditingConfig;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaAuditingConfig.class)
class AuthPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GitHubAccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndFindsAccountByStableGitHubId() {
        User user = userRepository.save(User.create());
        accountRepository.save(GitHubAccount.connect(
                user,
                1001L,
                "octocat",
                "octocat@example.com",
                Instant.parse("2026-08-18T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        GitHubAccount account = accountRepository.findByGithubUserId(1001L).orElseThrow();

        assertThat(account.getGithubUsername()).isEqualTo("octocat");
        assertThat(account.getUser().getId()).isEqualTo(user.getId());
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
    }
}
