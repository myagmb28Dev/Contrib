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

        var certificate = certificateService.create(principal.getUserId(), analysis.id(), SUBJECT);
        var intent = blockchainService.intent(principal.getUserId(), certificate.id());
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
