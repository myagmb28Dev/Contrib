package com.example.project.certificate.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.analysis.domain.ContributionAnalysis;
import com.example.project.analysis.domain.RepositorySnapshot;
import com.example.project.auth.domain.GitHubAccount;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.example.project.repository.domain.GitHubRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificatePayloadFactoryTest {

    private ObjectMapper objectMapper;
    private EthereumKeccak256 hasher;
    private CertificatePayloadFactory factory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        hasher = new EthereumKeccak256();
        factory = new CertificatePayloadFactory(objectMapper, hasher);
    }

    @Test
    void normalizesValidHexAddressToLowerCase() {
        String mixedCase = "0xAbCdEf1234567890aBcDeF1234567890AbCdEf12";
        String normalized = factory.normalizeAddress(mixedCase);
        assertThat(normalized).isEqualTo("0xabcdef1234567890abcdef1234567890abcdef12");
    }

    @Test
    void returnsNullForNullOrBlankAddress() {
        assertThat(factory.normalizeAddress(null)).isNull();
        assertThat(factory.normalizeAddress("")).isNull();
        assertThat(factory.normalizeAddress("   ")).isNull();
    }

    @Test
    void rejectsInvalidEthereumAddresses() {
        assertThatThrownBy(() -> factory.normalizeAddress("0x1234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20-byte hexadecimal Ethereum address");

        assertThatThrownBy(() -> factory.normalizeAddress("0xZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> factory.normalizeAddress("abcdef1234567890abcdef1234567890abcdef12"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUncompletedAnalysisJob() {
        RepositorySnapshot snapshot = mock(RepositorySnapshot.class);
        AnalysisJob job = mock(AnalysisJob.class);
        GitHubAccount account = mock(GitHubAccount.class);

        when(snapshot.getAnalysisJob()).thenReturn(job);
        when(job.getCompletedAt()).thenReturn(null);

        ContributionAnalysis analysis = mock(ContributionAnalysis.class);
        when(analysis.getSnapshot()).thenReturn(snapshot);

        assertThatThrownBy(() -> factory.create(analysis, account, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only a completed analysis can be certified");
    }

    @Test
    void generatesConsistentDeterministicHash() {
        RepositorySnapshot snapshot = mock(RepositorySnapshot.class);
        GitHubRepository repository = mock(GitHubRepository.class);
        AnalysisJob job = mock(AnalysisJob.class);
        GitHubAccount account = mock(GitHubAccount.class);

        Instant start = Instant.parse("2026-08-01T00:00:00Z");
        Instant end = Instant.parse("2026-09-01T00:00:00Z");
        Instant completedAt = Instant.parse("2026-09-01T01:00:00Z");

        when(snapshot.getRepository()).thenReturn(repository);
        when(snapshot.getAnalysisJob()).thenReturn(job);
        when(snapshot.getPeriodStart()).thenReturn(start);
        when(snapshot.getPeriodEnd()).thenReturn(end);
        when(snapshot.getCollectorVersion()).thenReturn("v1");
        when(snapshot.getSnapshotHash()).thenReturn("0x" + "1".repeat(64));
        when(job.getCompletedAt()).thenReturn(completedAt);

        when(repository.getGithubRepositoryId()).thenReturn(100L);
        when(repository.getFullName()).thenReturn("sample/repo");
        when(repository.getHtmlUrl()).thenReturn("https://github.com/sample/repo");
        when(repository.getDefaultBranch()).thenReturn("main");

        when(account.getGithubUserId()).thenReturn(200L);
        when(account.getGithubUsername()).thenReturn("sampleuser");

        ContributionAnalysis analysis = ContributionAnalysis.create(snapshot, "{\"commits\":10}", 85, "score-v1", "rules", "[\"Backend\"]");

        CanonicalCertificate cert1 = factory.create(analysis, account, "0x" + "a".repeat(40));
        CanonicalCertificate cert2 = factory.create(analysis, account, "0x" + "A".repeat(40));

        assertThat(cert1.json()).isEqualTo(cert2.json());
        assertThat(cert1.hash()).isEqualTo(cert2.hash());
        assertThat(cert1.hash()).isEqualTo(hasher.hashUtf8(cert1.json()));
    }
}
