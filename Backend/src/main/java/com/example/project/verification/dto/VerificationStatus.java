package com.example.project.verification.dto;

public enum VerificationStatus {
    VALID,
    HASH_MISMATCH,
    REVOKED,
    NOT_FOUND,
    PENDING,
    CHAIN_UNAVAILABLE
}
