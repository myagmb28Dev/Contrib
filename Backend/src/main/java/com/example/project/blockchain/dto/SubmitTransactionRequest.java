package com.example.project.blockchain.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitTransactionRequest(@NotBlank String transactionHash, @NotBlank String issuerWalletAddress) {
}
