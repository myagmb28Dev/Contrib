package com.example.project.blockchain.dto;

public record OnchainAttestationData(String certificateHash, String issuer, String subject,
        long issuedAt, long revokedAt) {
}
