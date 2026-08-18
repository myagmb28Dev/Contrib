package com.example.project.certificate.domain;

import java.time.Instant;
import java.util.UUID;

import com.example.project.analysis.domain.ContributionAnalysis;
import com.example.project.auth.domain.User;
import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "certificate")
public class Certificate extends BaseTimeEntity {

    @Id
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false, unique = true)
    private ContributionAnalysis analysis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "schema_version", nullable = false)
    private String schemaVersion;

    @Column(name = "canonical_payload", nullable = false, columnDefinition = "TEXT")
    private String canonicalPayload;

    @Column(name = "certificate_hash", nullable = false, unique = true, length = 66)
    private String certificateHash;

    @Column(name = "issuer_wallet_address", length = 42)
    private String issuerWalletAddress;

    @Column(name = "subject_wallet_address", length = 42)
    private String subjectWalletAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    protected Certificate() {
    }

    public static Certificate issue(ContributionAnalysis analysis, User user, String schemaVersion,
            String canonicalPayload, String certificateHash, String subjectWalletAddress, Instant issuedAt) {
        Certificate certificate = new Certificate();
        certificate.id = UUID.randomUUID();
        certificate.publicId = UUID.randomUUID();
        certificate.analysis = analysis;
        certificate.user = user;
        certificate.schemaVersion = schemaVersion;
        certificate.canonicalPayload = canonicalPayload;
        certificate.certificateHash = certificateHash;
        certificate.subjectWalletAddress = subjectWalletAddress;
        certificate.status = CertificateStatus.ISSUED;
        certificate.issuedAt = issuedAt;
        return certificate;
    }

    public void setIssuerWalletAddress(String issuerWalletAddress) { this.issuerWalletAddress = issuerWalletAddress; }
    public void revoke(String reason) {
        this.status = CertificateStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    public UUID getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public ContributionAnalysis getAnalysis() { return analysis; }
    public User getUser() { return user; }
    public String getSchemaVersion() { return schemaVersion; }
    public String getCanonicalPayload() { return canonicalPayload; }
    public String getCertificateHash() { return certificateHash; }
    public String getIssuerWalletAddress() { return issuerWalletAddress; }
    public String getSubjectWalletAddress() { return subjectWalletAddress; }
    public CertificateStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevocationReason() { return revocationReason; }
}
