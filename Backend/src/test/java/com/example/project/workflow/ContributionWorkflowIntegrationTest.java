package com.example.project.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.example.project.analysis.dto.AnalysisJobResponse;
import com.example.project.analysis.repository.AnalysisJobRepository;
import com.example.project.analysis.service.AnalysisService;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.example.project.auth.service.GitHubAccountConnectionService;
import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.auth.service.GitHubProfile;
import com.example.project.blockchain.client.EthereumJsonRpcClient;
import com.example.project.blockchain.dto.OnchainAttestationData;
import com.example.project.blockchain.dto.TransactionReceiptData;
import com.example.project.blockchain.service.BlockchainService;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.example.project.certificate.service.CertificateService;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.github.dto.GitHubUserDto;
import com.example.project.repository.service.RepositoryService;
import com.example.project.verification.dto.VerificationStatus;
import com.example.project.verification.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.token-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.analysis.dispatch-enabled=false",
        "app.ai.provider=rule-based",
        "app.blockchain.receipt-poll-enabled=false",
        "app.blockchain.contract-address=0x1111111111111111111111111111111111111111",
        "app.blockchain.rpc-url=http://localhost:8545"
})
class ContributionWorkflowIntegrationTest {

    private static final String CONTRACT = "0x1111111111111111111111111111111111111111";
    private static final String ISSUER = "0x2222222222222222222222222222222222222222";
    private static final String SUBJECT = "0x3333333333333333333333333333333333333333";
    private static final String ISSUE_TX = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String REVOKE_TX = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Autowired private GitHubAccountConnectionService accountConnectionService;
    @Autowired private RepositoryService repositoryService;
    @Autowired private AnalysisService analysisService;
    @Autowired private AnalysisJobRepository jobRepository;
    @Autowired private CertificateService certificateService;
    @Autowired private BlockchainService blockchainService;
    @Autowired private VerificationService verificationService;
    @Autowired private EthereumKeccak256 hasher;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private GitHubApiClient gitHubApiClient;
    @MockitoBean private GitHubAccessTokenService accessTokenService;
    @MockitoBean private EthereumJsonRpcClient rpcClient;

    @Test
    void completesOAuthCollectionAnalysisCertificateAttestationVerificationAndRevocation() throws Exception {
        GitHubPrincipal principal = accountConnectionService.connect(
                new GitHubProfile(1001L, "octocat", "octocat@example.com"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(accessTokenService.getValidAccessToken(principal.getUserId())).thenReturn("token");
        GitHubUserDto owner = new GitHubUserDto(1001L, "octocat");
        when(gitHubApiClient.getRepository("token", "octocat", "demo")).thenReturn(
                new GitHubRepositoryDto(2001L, owner, "demo", "octocat/demo",
                        "https://github.com/octocat/demo", false, "main", "Java", false));
        when(gitHubApiClient.getPublicRepositories("token")).thenReturn(List.of(
                new GitHubRepositoryDto(2001L, owner, "demo", "octocat/demo",
                        "https://github.com/octocat/demo", false, "main", "Java", false)));
        when(gitHubApiClient.getCommits(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of());
        when(gitHubApiClient.getPullRequests(anyString(), anyString(), anyString())).thenReturn(List.of());
        when(gitHubApiClient.getReviews(anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of());

        var repository = repositoryService.synchronize(principal.getUserId()).get(0);
        AnalysisJobResponse created = analysisService.create(principal.getUserId(), repository.id(),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"));
        waitForCompletion(created.id());
        var analysis = analysisService.listForRepository(principal.getUserId(), repository.id()).get(0);
        assertThat(analysis.scoreVersion()).isEqualTo("score-v1");
        assertThat(analysis.activityComparison().path("status").asText()).isEqualTo("NO_ACTIVITY");

        var certificate = certificateService.create(principal.getUserId(), analysis.id(), SUBJECT);
        assertThat(certificate.schemaVersion()).isEqualTo("1.1");
        assertThat(certificate.payload().path("result").path("activityComparison").path("modelVersion").asText())
                .isEqualTo("activity-percentile-v1");
        assertThat(verificationService.verify(certificate.publicId()).status())
                .isEqualTo(VerificationStatus.NOT_REGISTERED);
        var intent = blockchainService.intent(principal.getUserId(), certificate.id());
        when(rpcClient.getTransactionReceipt(ISSUE_TX)).thenReturn(null);
        assertThat(blockchainService.submit(principal.getUserId(), certificate.id(), ISSUE_TX, ISSUER).status())
                .isEqualTo("PENDING");
        assertThat(verificationService.verify(certificate.publicId()).status()).isEqualTo(VerificationStatus.PENDING);
        when(rpcClient.getTransactionReceipt(ISSUE_TX)).thenReturn(issueReceipt(
                ISSUE_TX, intent.onchainCertificateId(), certificate.hash()));
        var attestation = blockchainService.submit(principal.getUserId(), certificate.id(), ISSUE_TX, ISSUER);
        assertThat(attestation.status()).isEqualTo("CONFIRMED");

        when(rpcClient.getAttestation(CONTRACT, intent.onchainCertificateId()))
                .thenReturn(new OnchainAttestationData(certificate.hash(), ISSUER, SUBJECT, 1L, 0L));
        assertThat(verificationService.verify(certificate.publicId()).status()).isEqualTo(VerificationStatus.VALID);

        var revocationIntent = blockchainService.revocationIntent(principal.getUserId(), certificate.id());
        when(rpcClient.getTransactionReceipt(REVOKE_TX))
                .thenReturn(revocationReceipt(REVOKE_TX, revocationIntent.onchainCertificateId()));
        var revoked = blockchainService.submitRevocation(principal.getUserId(), certificate.id(),
                REVOKE_TX, ISSUER, "Superseded certificate");
        assertThat(revoked.revocationStatus()).isEqualTo("CONFIRMED");
        assertThat(verificationService.verify(certificate.publicId()).status()).isEqualTo(VerificationStatus.REVOKED);
    }

    @Test
    void selectsPrivateRepositoriesAndPreservesThemDuringPublicSync() {
        var principal = accountConnectionService.connect(
                new GitHubProfile(9001L, "private-user", "private@example.com"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var userId = principal.getUserId();
        when(accessTokenService.getValidAccessToken(userId)).thenReturn("private-token");
        var owner = new GitHubUserDto(9001L, "private-user");
        var privateRepo = new GitHubRepositoryDto(9002L, owner, "secret", "private-user/secret",
                "https://github.com/private-user/secret", true, "main", "Java", false);
        var publicRepo = new GitHubRepositoryDto(9003L, owner, "public", "private-user/public",
                "https://github.com/private-user/public", false, "main", "Java", false);
        when(gitHubApiClient.getAccessibleRepositories("private-token"))
                .thenReturn(List.of(privateRepo, publicRepo));
        assertThat(repositoryService.listAvailableFromGitHub(userId)).containsExactly(privateRepo, publicRepo);
        assertThat(repositoryService.syncSelected(userId, List.of(9002L)))
                .singleElement().satisfies(repo -> assertThat(repo.visibility()).isEqualTo("PRIVATE"));
        when(gitHubApiClient.getPublicRepositories("private-token")).thenReturn(List.of(publicRepo));
        assertThat(repositoryService.synchronize(userId))
                .extracting(repo -> repo.visibility()).containsExactlyInAnyOrder("PRIVATE", "PUBLIC");
    }

    @Test
    void persistsPublishedComparisonIntoANewCertificate() throws Exception {
        var principal = accountConnectionService.connect(new GitHubProfile(22001L, "comparison-user", null),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(accessTokenService.getValidAccessToken(principal.getUserId())).thenReturn("comparison-token");
        var owner = new GitHubUserDto(22001L, "comparison-user", "User");
        var repoDto = new GitHubRepositoryDto(22002L, owner, "comparison", "comparison-user/comparison",
                "https://github.com/comparison-user/comparison", false, "main", "C#", false);
        when(gitHubApiClient.getPublicRepositories("comparison-token")).thenReturn(List.of(repoDto));
        when(gitHubApiClient.getRepository("comparison-token", "comparison-user", "comparison")).thenReturn(repoDto);
        var start = Instant.parse("2026-06-01T00:00:00Z");
        var end = Instant.parse("2026-09-01T00:00:00Z");
        var commits = new java.util.ArrayList<com.example.project.github.dto.GitHubCommitDto>();
        for (int i = 0; i < 14; i++) {
            var author = new GitHubUserDto(22001L + i, "person-" + i, "User");
            commits.add(new com.example.project.github.dto.GitHubCommitDto("commit-" + i, author,
                    new com.example.project.github.dto.GitHubCommitDto.CommitData(
                            new com.example.project.github.dto.GitHubCommitDto.CommitAuthor(start.plusSeconds(1)), "activity"), List.of()));
        }
        when(gitHubApiClient.getCommits("comparison-token", "comparison-user", "comparison", "main", start, end)).thenReturn(commits);
        when(gitHubApiClient.getCommit("comparison-token", "comparison-user", "comparison", "commit-0"))
                .thenReturn(new com.example.project.github.dto.GitHubCommitDetailDto("commit-0",
                        new com.example.project.github.dto.GitHubCommitDetailDto.Stats(1, 0, 1), List.of()));
        var repository = repositoryService.synchronize(principal.getUserId()).get(0);
        var created = analysisService.create(principal.getUserId(), repository.id(), start, end);
        waitForCompletion(created.id());
        var analysis = analysisService.listForRepository(principal.getUserId(), repository.id()).get(0);
        assertThat(analysis.activityComparison().path("status").asText()).isEqualTo("AVAILABLE");
        assertThat(analysis.activityComparison().path("contributorCount").asInt()).isGreaterThanOrEqualTo(20);
        assertThat(analysisService.create(principal.getUserId(), repository.id(), start, end).id()).isEqualTo(created.id());
        var certificate = certificateService.create(principal.getUserId(), analysis.id(), null);
        assertThat(certificate.payload().path("result").path("activityComparison")).isEqualTo(analysis.activityComparison());
        assertThat(certificateService.create(principal.getUserId(), analysis.id(), null).hash()).isEqualTo(certificate.hash());
    }

    private void waitForCompletion(java.util.UUID jobId) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            var job = jobRepository.findById(jobId).orElseThrow();
            if (job.getStatus().name().equals("COMPLETED")) return;
            if (job.getStatus().name().equals("FAILED")) {
                throw new AssertionError("Analysis failed: " + job.getErrorMessage());
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Analysis did not complete in time");
    }

    private TransactionReceiptData issueReceipt(String transactionHash, String onchainId, String certificateHash)
            throws Exception {
        String eventTopic = hasher.hashUtf8("CertificateIssued(bytes32,bytes32,address,address)");
        String logs = """
                [{"address":"%s","topics":["%s","%s","%s","%s"],"data":"%s"}]
                """.formatted(CONTRACT, eventTopic, onchainId, topic(ISSUER), topic(SUBJECT), certificateHash);
        return new TransactionReceiptData(transactionHash, CONTRACT, true, 42L, objectMapper.readTree(logs));
    }

    private TransactionReceiptData revocationReceipt(String transactionHash, String onchainId) throws Exception {
        String eventTopic = hasher.hashUtf8("CertificateRevoked(bytes32,address,uint64)");
        String logs = """
                [{"address":"%s","topics":["%s","%s","%s"],"data":"0x%s"}]
                """.formatted(CONTRACT, eventTopic, onchainId, topic(ISSUER), "0".repeat(63) + "1");
        return new TransactionReceiptData(transactionHash, CONTRACT, true, 43L, objectMapper.readTree(logs));
    }

    private String topic(String address) {
        return "0x" + "0".repeat(24) + address.substring(2);
    }
}
