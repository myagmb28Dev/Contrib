package com.example.project.blockchain.dto;

import java.util.List;
import java.util.UUID;

public record AttestationIntentResponse(
        UUID certificateId,
        long chainId,
        String network,
        String contractAddress,
        String functionName,
        List<String> arguments,
        String onchainCertificateId,
        String certificateHash) {
}
