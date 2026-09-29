package com.example.project.analysis.benchmark;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.example.project.analysis.domain.*;
import com.example.project.auth.domain.GitHubAccount;
import com.example.project.repository.domain.GitHubRepository;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.example.project.certificate.payload.CertificatePayloadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

class ComparisonCertificateTest {
    @Test void freezesComparisonAndRetainsLegacyPayloadWithoutInventingPercentiles() throws Exception {
        var snapshot = mock(RepositorySnapshot.class);
        var repo = mock(GitHubRepository.class);
        var job = mock(AnalysisJob.class);
        var account = mock(GitHubAccount.class);
        when(snapshot.getRepository()).thenReturn(repo);
        when(snapshot.getAnalysisJob()).thenReturn(job);
        when(snapshot.getPeriodStart()).thenReturn(PercentileCalculatorTest.START);
        when(snapshot.getPeriodEnd()).thenReturn(PercentileCalculatorTest.END);
        when(job.getCompletedAt()).thenReturn(PercentileCalculatorTest.END.plusSeconds(1));
        when(repo.getFullName()).thenReturn("owner/demo");
        when(account.getGithubUsername()).thenReturn("person");
        var mapper = new ObjectMapper().findAndRegisterModules();
        var factory = new CertificatePayloadFactory(mapper, new EthereumKeccak256());
        var analysis = ContributionAnalysis.create(snapshot, "{}", 0, "score-v1", "rules", "[]");
        var legacy = factory.create(analysis, account, null);
        assertThat(legacy.payload().get("schemaVersion")).isEqualTo("1.0");
        assertThat(mapper.readTree(legacy.json()).path("result").has("activityComparison")).isFalse();
        assertThat(factory.create(analysis, account, null).hash()).isEqualTo(legacy.hash());
        var comparison = new PercentileCalculator().calculate(PercentileCalculatorTest.context(999),
                PercentileCalculatorTest.vector(5), PercentileCalculatorTest.dataset(false), "data-original");
        analysis.setActivityComparison(mapper.writeValueAsString(comparison));
        var frozen = factory.create(analysis, account, null);
        assertThat(frozen.payload().get("schemaVersion")).isEqualTo("1.1");
        assertThat(mapper.readTree(frozen.json()).path("result").path("activityComparison").path("datasetId").asText())
                .isEqualTo("data-original");
        assertThatThrownBy(() -> analysis.setActivityComparison("{}")) .isInstanceOf(IllegalStateException.class);
        assertThat(factory.create(analysis, account, null).hash()).isEqualTo(frozen.hash());
        assertThat(frozen.hash()).isNotEqualTo(legacy.hash());
    }
}
