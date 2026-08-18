package com.example.project.certificate.payload;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.project.analysis.domain.ContributionAnalysis;
import com.example.project.auth.domain.GitHubAccount;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class CertificatePayloadFactory {

    public static final String SCHEMA_VERSION = "1.0";

    private final ObjectMapper objectMapper;
    private final EthereumKeccak256 hasher;

    public CertificatePayloadFactory(ObjectMapper objectMapper, EthereumKeccak256 hasher) {
        this.objectMapper = objectMapper;
        this.hasher = hasher;
    }

    public CanonicalCertificate create(ContributionAnalysis analysis, GitHubAccount account,
            String subjectWalletAddress) {
        var snapshot = analysis.getSnapshot();
        var job = snapshot.getAnalysisJob();
        var repository = snapshot.getRepository();
        if (job.getCompletedAt() == null) {
            throw new IllegalArgumentException("Only a completed analysis can be certified");
        }

        Map<String, Object> subject = ordered(
                "githubId", account.getGithubUserId(),
                "githubUsername", account.getGithubUsername(),
                "walletAddress", normalizeAddress(subjectWalletAddress));
        Map<String, Object> repositoryPayload = ordered(
                "githubRepositoryId", repository.getGithubRepositoryId(),
                "fullName", repository.getFullName(),
                "url", repository.getHtmlUrl(),
                "defaultBranch", repository.getDefaultBranch());
        Map<String, Object> period = ordered(
                "start", snapshot.getPeriodStart().toString(),
                "end", snapshot.getPeriodEnd().toString());
        Map<String, Object> source = ordered(
                "collectorVersion", snapshot.getCollectorVersion(),
                "snapshotHash", snapshot.getSnapshotHash());
        Map<String, Object> result = ordered(
                "metrics", jsonValue(analysis.getMetrics()),
                "score", analysis.getScore(),
                "scoreVersion", analysis.getScoreVersion(),
                "calculationRules", analysis.getCalculationRules(),
                "technicalAreas", jsonValue(analysis.getTechnicalAreas()),
                "summary", analysis.getAiSummary());

        Instant issuedAt = job.getCompletedAt();
        Map<String, Object> payload = ordered(
                "schemaVersion", SCHEMA_VERSION,
                "subject", subject,
                "repository", repositoryPayload,
                "period", period,
                "source", source,
                "result", result,
                "issuedAt", issuedAt.toString());
        String json = json(payload);
        return new CanonicalCertificate(json, hasher.hashUtf8(json), payload, issuedAt);
    }

    public String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String normalized = address.toLowerCase();
        if (!normalized.matches("0x[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Wallet address must be a 20-byte hexadecimal Ethereum address");
        }
        return normalized;
    }

    private Object jsonValue(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored analysis JSON is invalid", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize certificate payload", exception);
        }
    }

    private Map<String, Object> ordered(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            map.put((String) keyValues[index], keyValues[index + 1]);
        }
        return map;
    }
}
