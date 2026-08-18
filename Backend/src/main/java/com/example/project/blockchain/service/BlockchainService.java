package com.example.project.blockchain.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.project.blockchain.client.BlockchainRpcException;
import com.example.project.blockchain.client.EthereumJsonRpcClient;
import com.example.project.blockchain.config.BlockchainProperties;
import com.example.project.blockchain.domain.BlockchainAttestation;
import com.example.project.blockchain.dto.AttestationIntentResponse;
import com.example.project.blockchain.dto.AttestationResponse;
import com.example.project.blockchain.dto.TransactionReceiptData;
import com.example.project.blockchain.repository.BlockchainAttestationRepository;
import com.example.project.certificate.domain.Certificate;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.example.project.certificate.payload.CertificatePayloadFactory;
import com.example.project.certificate.repository.CertificateRepository;
import com.example.project.common.exception.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockchainService {

    private static final String ISSUE_EVENT = "CertificateIssued(bytes32,bytes32,address,address)";

    private final BlockchainProperties properties;
    private final CertificateRepository certificateRepository;
    private final BlockchainAttestationRepository attestationRepository;
    private final CertificatePayloadFactory payloadFactory;
    private final EthereumKeccak256 hasher;
    private final EthereumJsonRpcClient rpcClient;

    public BlockchainService(BlockchainProperties properties, CertificateRepository certificateRepository,
            BlockchainAttestationRepository attestationRepository, CertificatePayloadFactory payloadFactory,
            EthereumKeccak256 hasher, EthereumJsonRpcClient rpcClient) {
        this.properties = properties;
        this.certificateRepository = certificateRepository;
        this.attestationRepository = attestationRepository;
        this.payloadFactory = payloadFactory;
        this.hasher = hasher;
        this.rpcClient = rpcClient;
    }

    @Transactional(readOnly = true)
    public AttestationIntentResponse intent(UUID userId, UUID certificateId) {
        Certificate certificate = ownedCertificate(userId, certificateId);
        ensureConfigured();
        if (certificate.getSubjectWalletAddress() == null) {
            throw new IllegalArgumentException("A subject wallet address is required for blockchain attestation");
        }
        String onchainId = onchainId(certificate);
        return new AttestationIntentResponse(certificate.getId(), properties.getChainId(), properties.getNetwork(),
                properties.getContractAddress().toLowerCase(Locale.ROOT), "issue",
                List.of(onchainId, certificate.getCertificateHash(), certificate.getSubjectWalletAddress()),
                onchainId, certificate.getCertificateHash());
    }

    @Transactional
    public AttestationResponse submit(UUID userId, UUID certificateId, String transactionHash,
            String issuerWalletAddress) {
        Certificate certificate = ownedCertificate(userId, certificateId);
        ensureConfigured();
        String txHash = requireHash(transactionHash, "Transaction hash");
        String issuer = payloadFactory.normalizeAddress(issuerWalletAddress);
        var existing = attestationRepository.findByCertificateId(certificateId);
        if (existing.isPresent()) {
            if (!existing.get().getTransactionHash().equalsIgnoreCase(txHash)) {
                throw new IllegalArgumentException("Certificate already has another transaction");
            }
            refresh(existing.get());
            return AttestationResponse.from(existing.get());
        }
        certificate.setIssuerWalletAddress(issuer);
        BlockchainAttestation attestation = BlockchainAttestation.pending(certificate, properties.getChainId(),
                properties.getNetwork(), properties.getContractAddress().toLowerCase(Locale.ROOT),
                onchainId(certificate), txHash);
        attestationRepository.save(attestation);
        refresh(attestation);
        return AttestationResponse.from(attestation);
    }

    @Transactional
    public AttestationResponse get(UUID userId, String transactionHash) {
        BlockchainAttestation attestation = attestationRepository.findByTransactionHash(transactionHash)
                .orElseThrow(() -> new ResourceNotFoundException("Blockchain transaction not found"));
        if (!attestation.getCertificate().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Blockchain transaction not found");
        }
        refresh(attestation);
        return AttestationResponse.from(attestation);
    }

    public boolean refresh(BlockchainAttestation attestation) {
        if (attestation.getStatus() != com.example.project.blockchain.domain.AttestationStatus.PENDING) {
            return attestation.getStatus() == com.example.project.blockchain.domain.AttestationStatus.CONFIRMED;
        }
        try {
            TransactionReceiptData receipt = rpcClient.getTransactionReceipt(attestation.getTransactionHash());
            if (receipt == null) {
                return false;
            }
            if (!receipt.successful() || !receipt.contractAddress().equalsIgnoreCase(attestation.getContractAddress())
                    || !hasExpectedIssueLog(receipt, attestation)) {
                attestation.fail();
                return false;
            }
            attestation.confirm(receipt.blockNumber());
            return true;
        } catch (BlockchainRpcException exception) {
            return false;
        }
    }

    private boolean hasExpectedIssueLog(TransactionReceiptData receipt, BlockchainAttestation attestation) {
        if (!receipt.logs().isArray()) {
            return false;
        }
        String eventTopic = hasher.hashUtf8(ISSUE_EVENT);
        for (var log : receipt.logs()) {
            if (!log.path("address").asText().equalsIgnoreCase(attestation.getContractAddress())) {
                continue;
            }
            var topics = log.path("topics");
            if (topics.size() != 4 || !topics.get(0).asText().equalsIgnoreCase(eventTopic)
                    || !topics.get(1).asText().equalsIgnoreCase(attestation.getOnchainCertificateId())
                    || !topicAddress(topics.get(2).asText()).equalsIgnoreCase(
                            attestation.getCertificate().getIssuerWalletAddress())
                    || !topicAddress(topics.get(3).asText()).equalsIgnoreCase(
                            attestation.getCertificate().getSubjectWalletAddress())) {
                continue;
            }
            String data = log.path("data").asText();
            String encodedHash = attestation.getCertificate().getCertificateHash();
            return data.equalsIgnoreCase(encodedHash);
        }
        return false;
    }

    private String topicAddress(String topic) {
        return "0x" + topic.substring(topic.length() - 40);
    }

    private String onchainId(Certificate certificate) {
        return hasher.hashUtf8(certificate.getPublicId().toString());
    }

    private String requireHash(String value, String label) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("0x[0-9a-f]{64}")) {
            throw new IllegalArgumentException(label + " must be a 32-byte hexadecimal value");
        }
        return normalized;
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Blockchain contract address and RPC URL are not configured");
        }
    }

    private Certificate ownedCertificate(UUID userId, UUID certificateId) {
        return certificateRepository.findByIdAndUserId(certificateId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
    }
}
