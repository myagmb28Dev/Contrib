package com.example.project.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.analysis.domain.AnalysisJobStatus;
import com.example.project.analysis.repository.AnalysisJobRepository;
import com.example.project.auth.domain.User;
import com.example.project.auth.repository.UserRepository;
import com.example.project.repository.domain.GitHubRepository;
import com.example.project.repository.repository.GitHubRepositoryRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:analysis-queue;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.analysis.dispatch-enabled=false",
        "app.blockchain.receipt-poll-enabled=false"
})
@Transactional
class AnalysisJobQueueIntegrationTest {

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GitHubRepositoryRepository repositoryRepository;

    @Test
    void reclaimsAnExpiredDatabaseLeaseAfterRestart() {
        User user = userRepository.save(User.create());
        GitHubRepository repository = GitHubRepository.create(user, 42L);
        repository.synchronize(7L, "octocat", "demo", "octocat/demo", "https://github.com/octocat/demo",
                "main", "Java", false, Instant.now());
        repositoryRepository.save(repository);
        AnalysisJob job = jobRepository.saveAndFlush(AnalysisJob.create(user, repository,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"), "github-v1"));
        Instant now = Instant.now();

        assertThat(jobRepository.findClaimableIds(now, PageRequest.of(0, 20))).contains(job.getId());
        assertThat(jobRepository.claim(job.getId(), now, now.minusSeconds(1))).isEqualTo(1);
        AnalysisJob firstClaim = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(firstClaim.getStatus()).isEqualTo(AnalysisJobStatus.COLLECTING);
        assertThat(firstClaim.getAttemptCount()).isEqualTo(1);

        assertThat(jobRepository.findClaimableIds(now, PageRequest.of(0, 20))).contains(job.getId());
        assertThat(jobRepository.claim(job.getId(), now, now.plusSeconds(300))).isEqualTo(1);
        AnalysisJob recovered = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(recovered.getAttemptCount()).isEqualTo(2);
        assertThat(recovered.getLeaseExpiresAt()).isAfter(now);
    }
}
