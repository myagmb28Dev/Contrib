package com.example.project.blockchain.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.project.blockchain.domain.BlockchainAttestation;

public record AttestationResponse(
        UUID id,
        UUID certificateId,
        long chainId,
        String network,
        String contractAddress,
        String onchainCertificateId,
        String transactionHash,
        Long blockNumber,
        String status,
        Instant submittedAt,
        Instant confirmedAt,
        String revocationTransactionHash,
        Long revocationBlockNumber,
        String revocationStatus,
        String revocationReason,
        Instant revocationSubmittedAt,
        Instant revocationConfirmedAt) {

    public static AttestationResponse from(BlockchainAttestation attestation) {
        return new AttestationResponse(attestation.getId(), attestation.getCertificate().getId(),
                attestation.getChainId(), attestation.getNetwork(), attestation.getContractAddress(),
                attestation.getOnchainCertificateId(), attestation.getTransactionHash(),
                attestation.getBlockNumber(), attestation.getStatus().name(), attestation.getSubmittedAt(),
                attestation.getConfirmedAt(), attestation.getRevocationTransactionHash(),
                attestation.getRevocationBlockNumber(),
                attestation.getRevocationStatus() == null ? null : attestation.getRevocationStatus().name(),
                attestation.getRevocationReason(), attestation.getRevocationSubmittedAt(),
                attestation.getRevocationConfirmedAt());
    }
}
