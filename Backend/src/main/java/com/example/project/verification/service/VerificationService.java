package com.example.project.verification.service;

import java.util.UUID;

import com.example.project.blockchain.client.BlockchainRpcException;
import com.example.project.blockchain.client.EthereumJsonRpcClient;
import com.example.project.blockchain.domain.AttestationStatus;
import com.example.project.blockchain.repository.BlockchainAttestationRepository;
import com.example.project.blockchain.service.BlockchainService;
import com.example.project.certificate.domain.Certificate;
import com.example.project.certificate.domain.CertificateStatus;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.example.project.certificate.repository.CertificateRepository;
import com.example.project.verification.dto.VerificationResponse;
import com.example.project.verification.dto.VerificationStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {

    private final CertificateRepository certificateRepository;
    private final BlockchainAttestationRepository attestationRepository;
    private final EthereumKeccak256 hasher;
    private final EthereumJsonRpcClient rpcClient;
    private final BlockchainService blockchainService;

    public VerificationService(CertificateRepository certificateRepository,
            BlockchainAttestationRepository attestationRepository, EthereumKeccak256 hasher,
            EthereumJsonRpcClient rpcClient, BlockchainService blockchainService) {
        this.certificateRepository = certificateRepository;
        this.attestationRepository = attestationRepository;
        this.hasher = hasher;
        this.rpcClient = rpcClient;
        this.blockchainService = blockchainService;
    }

    public VerificationResponse verify(UUID publicId) {
        var optional = certificateRepository.findByPublicId(publicId);
        if (optional.isEmpty()) {
            return response(publicId, VerificationStatus.NOT_FOUND, null, null, null, "Certificate was not found");
        }
        Certificate certificate = optional.get();
        String calculated = hasher.hashUtf8(certificate.getCanonicalPayload());
        if (!calculated.equalsIgnoreCase(certificate.getCertificateHash())) {
            return response(publicId, VerificationStatus.HASH_MISMATCH, certificate.getCertificateHash(),
                    calculated, null, "Payload hash does not match");
        }
        var attestation = attestationRepository.findByCertificateId(certificate.getId());
        attestation.ifPresent(value -> blockchainService.refreshPending(value.getId()));
        attestation = attestationRepository.findByCertificateId(certificate.getId());
        String txHash = attestation.map(value -> value.getTransactionHash()).orElse(null);
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            return response(publicId, VerificationStatus.REVOKED, certificate.getCertificateHash(),
                    calculated, txHash, certificate.getRevocationReason());
        }
        if (attestation.isEmpty() || attestation.get().getStatus() == AttestationStatus.PENDING) {
            return response(publicId, VerificationStatus.PENDING, certificate.getCertificateHash(),
                    calculated, txHash, "Blockchain attestation is pending");
        }
        if (attestation.get().getStatus() == AttestationStatus.FAILED) {
            return response(publicId, VerificationStatus.HASH_MISMATCH, certificate.getCertificateHash(),
                    calculated, txHash, "Blockchain transaction did not contain the expected attestation");
        }
        try {
            var chain = rpcClient.getAttestation(attestation.get().getContractAddress(),
                    attestation.get().getOnchainCertificateId());
            if (!chain.certificateHash().equalsIgnoreCase(certificate.getCertificateHash())) {
                return response(publicId, VerificationStatus.HASH_MISMATCH, certificate.getCertificateHash(),
                        chain.certificateHash(), txHash, "On-chain hash does not match");
            }
            if (chain.revokedAt() > 0) {
                return response(publicId, VerificationStatus.REVOKED, certificate.getCertificateHash(),
                        calculated, txHash, "Certificate was revoked on-chain");
            }
            return response(publicId, VerificationStatus.VALID, certificate.getCertificateHash(),
                    calculated, txHash, "Payload, database, and on-chain hash match");
        } catch (BlockchainRpcException exception) {
            return response(publicId, VerificationStatus.CHAIN_UNAVAILABLE, certificate.getCertificateHash(),
                    calculated, txHash, exception.getMessage());
        }
    }

    private VerificationResponse response(UUID publicId, VerificationStatus status, String stored,
            String calculated, String transactionHash, String message) {
        return new VerificationResponse(publicId, status, stored, calculated, transactionHash, message);
    }
}
