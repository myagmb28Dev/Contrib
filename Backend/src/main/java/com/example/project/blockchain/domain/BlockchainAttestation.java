package com.example.project.blockchain.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.certificate.domain.Certificate;
import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "blockchain_attestation")
public class BlockchainAttestation extends BaseTimeEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false, unique = true)
    private Certificate certificate;

    @Column(name = "chain_id", nullable = false)
    private long chainId;

    @Column(nullable = false)
    private String network;

    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;

    @Column(name = "onchain_certificate_id", nullable = false, length = 66)
    private String onchainCertificateId;

    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttestationStatus status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "revocation_transaction_hash", unique = true, length = 66)
    private String revocationTransactionHash;

    @Column(name = "revocation_block_number")
    private Long revocationBlockNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_status")
    private AttestationStatus revocationStatus;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "revocation_submitted_at")
    private Instant revocationSubmittedAt;

    @Column(name = "revocation_confirmed_at")
    private Instant revocationConfirmedAt;

    protected BlockchainAttestation() {
    }

    public static BlockchainAttestation pending(Certificate certificate, long chainId, String network,
            String contractAddress, String onchainCertificateId, String transactionHash) {
        BlockchainAttestation attestation = new BlockchainAttestation();
        attestation.id = UUID.randomUUID();
        attestation.certificate = certificate;
        attestation.chainId = chainId;
        attestation.network = network;
        attestation.contractAddress = contractAddress;
        attestation.onchainCertificateId = onchainCertificateId;
        attestation.transactionHash = transactionHash;
        attestation.status = AttestationStatus.PENDING;
        attestation.submittedAt = Instant.now();
        return attestation;
    }

    public void confirm(long blockNumber) {
        this.blockNumber = blockNumber;
        this.status = AttestationStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void fail() { this.status = AttestationStatus.FAILED; }

    public void resubmit(String transactionHash) {
        this.transactionHash = transactionHash;
        this.blockNumber = null;
        this.status = AttestationStatus.PENDING;
        this.submittedAt = Instant.now();
        this.confirmedAt = null;
    }

    public void submitRevocation(String transactionHash, String reason) {
        this.revocationTransactionHash = transactionHash;
        this.revocationReason = reason;
        this.revocationStatus = AttestationStatus.PENDING;
        this.revocationSubmittedAt = Instant.now();
        this.revocationBlockNumber = null;
        this.revocationConfirmedAt = null;
    }

    public void confirmRevocation(long blockNumber) {
        this.revocationBlockNumber = blockNumber;
        this.revocationStatus = AttestationStatus.CONFIRMED;
        this.revocationConfirmedAt = Instant.now();
        this.certificate.revoke(revocationReason);
    }

    public void failRevocation() { this.revocationStatus = AttestationStatus.FAILED; }

    public UUID getId() { return id; }
    public Certificate getCertificate() { return certificate; }
    public long getChainId() { return chainId; }
    public String getNetwork() { return network; }
    public String getContractAddress() { return contractAddress; }
    public String getOnchainCertificateId() { return onchainCertificateId; }
    public String getTransactionHash() { return transactionHash; }
    public Long getBlockNumber() { return blockNumber; }
    public AttestationStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public String getRevocationTransactionHash() { return revocationTransactionHash; }
    public Long getRevocationBlockNumber() { return revocationBlockNumber; }
    public AttestationStatus getRevocationStatus() { return revocationStatus; }
    public String getRevocationReason() { return revocationReason; }
    public Instant getRevocationSubmittedAt() { return revocationSubmittedAt; }
    public Instant getRevocationConfirmedAt() { return revocationConfirmedAt; }
}
