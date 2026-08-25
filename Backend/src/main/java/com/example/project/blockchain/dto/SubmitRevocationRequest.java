package com.example.project.blockchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitRevocationRequest(
        @NotBlank String transactionHash,
        @NotBlank String issuerWalletAddress,
        @NotBlank @Size(max = 1000) String reason) {
}
