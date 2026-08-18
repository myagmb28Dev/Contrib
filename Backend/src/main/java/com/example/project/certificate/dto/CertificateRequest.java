package com.example.project.certificate.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CertificateRequest(@NotNull UUID analysisId, String subjectWalletAddress) {
}
