package com.example.project.blockchain.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record TransactionReceiptData(String transactionHash, String contractAddress, boolean successful,
        long blockNumber, JsonNode logs) {
}
