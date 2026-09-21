package com.example.project.verification.dto;

public enum VerificationStatus {
    VALID,
    HASH_MISMATCH,
    REVOKED,
    NOT_FOUND,
    NOT_REGISTERED,
    PENDING,
    CHAIN_UNAVAILABLE
}
