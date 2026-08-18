package com.example.project.verification.dto;

import java.util.UUID;

public record VerificationResponse(UUID publicId, VerificationStatus status, String storedHash,
        String calculatedHash, String transactionHash, String message) {
}
