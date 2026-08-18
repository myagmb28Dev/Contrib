package com.example.project.certificate.dto;

import java.time.Instant;

import com.example.project.certificate.payload.CanonicalCertificate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record CertificatePreviewResponse(JsonNode payload, String hash, Instant issuedAt) {
    public static CertificatePreviewResponse from(CanonicalCertificate certificate, ObjectMapper objectMapper) {
        try {
            return new CertificatePreviewResponse(objectMapper.readTree(certificate.json()),
                    certificate.hash(), certificate.issuedAt());
        } catch (Exception exception) {
            throw new IllegalStateException("Canonical certificate JSON is invalid", exception);
        }
    }
}
