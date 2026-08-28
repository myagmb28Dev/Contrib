package com.example.project.certificate.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.project.certificate.domain.Certificate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record CertificateResponse(
        UUID id,
        UUID publicId,
        UUID analysisId,
        String repositoryName,
        String repositoryFullName,
        String schemaVersion,
        JsonNode payload,
        String hash,
        String issuerWalletAddress,
        String subjectWalletAddress,
        String status,
        Instant issuedAt,
        Instant revokedAt,
        String revocationReason) {

    public static CertificateResponse from(Certificate certificate, ObjectMapper objectMapper) {
        try {
            var repo = certificate.getAnalysis().getSnapshot().getRepository();
            return new CertificateResponse(certificate.getId(), certificate.getPublicId(),
                    certificate.getAnalysis().getId(), repo.getName(), repo.getFullName(),
                    certificate.getSchemaVersion(),
                    objectMapper.readTree(certificate.getCanonicalPayload()), certificate.getCertificateHash(),
                    certificate.getIssuerWalletAddress(), certificate.getSubjectWalletAddress(),
                    certificate.getStatus().name(), certificate.getIssuedAt(), certificate.getRevokedAt(),
                    certificate.getRevocationReason());
        } catch (Exception exception) {
            throw new IllegalStateException("Stored certificate JSON is invalid", exception);
        }
    }
}
