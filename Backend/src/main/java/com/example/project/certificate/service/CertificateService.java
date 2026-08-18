package com.example.project.certificate.service;

import java.util.List;
import java.util.UUID;

import com.example.project.analysis.domain.ContributionAnalysis;
import com.example.project.analysis.repository.ContributionAnalysisRepository;
import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.repository.GitHubAccountRepository;
import com.example.project.certificate.domain.Certificate;
import com.example.project.certificate.domain.CertificateStatus;
import com.example.project.certificate.dto.CertificatePreviewResponse;
import com.example.project.certificate.dto.CertificateResponse;
import com.example.project.certificate.payload.CanonicalCertificate;
import com.example.project.certificate.payload.CertificatePayloadFactory;
import com.example.project.certificate.repository.CertificateRepository;
import com.example.project.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateService {

    private final ContributionAnalysisRepository analysisRepository;
    private final GitHubAccountRepository accountRepository;
    private final CertificateRepository certificateRepository;
    private final CertificatePayloadFactory payloadFactory;
    private final ObjectMapper objectMapper;

    public CertificateService(ContributionAnalysisRepository analysisRepository,
            GitHubAccountRepository accountRepository, CertificateRepository certificateRepository,
            CertificatePayloadFactory payloadFactory, ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.accountRepository = accountRepository;
        this.certificateRepository = certificateRepository;
        this.payloadFactory = payloadFactory;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CertificatePreviewResponse preview(UUID userId, UUID analysisId, String walletAddress) {
        return CertificatePreviewResponse.from(canonical(userId, analysisId, walletAddress), objectMapper);
    }

    @Transactional
    public CertificateResponse create(UUID userId, UUID analysisId, String walletAddress) {
        var existing = certificateRepository.findByAnalysisId(analysisId);
        if (existing.isPresent()) {
            if (!existing.get().getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Certificate not found");
            }
            return CertificateResponse.from(existing.get(), objectMapper);
        }
        ContributionAnalysis analysis = ownedAnalysis(userId, analysisId);
        CanonicalCertificate canonical = canonical(userId, analysis, walletAddress);
        Certificate certificate = Certificate.issue(analysis, analysis.getSnapshot().getAnalysisJob().getUser(),
                CertificatePayloadFactory.SCHEMA_VERSION, canonical.json(), canonical.hash(),
                payloadFactory.normalizeAddress(walletAddress), canonical.issuedAt());
        return CertificateResponse.from(certificateRepository.save(certificate), objectMapper);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> list(UUID userId) {
        return certificateRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(certificate -> CertificateResponse.from(certificate, objectMapper)).toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse get(UUID userId, UUID id) {
        return CertificateResponse.from(ownedCertificate(userId, id), objectMapper);
    }

    @Transactional
    public CertificateResponse revoke(UUID userId, UUID id, String reason) {
        Certificate certificate = ownedCertificate(userId, id);
        if (certificate.getStatus() != CertificateStatus.REVOKED) {
            certificate.revoke(reason);
        }
        return CertificateResponse.from(certificate, objectMapper);
    }

    @Transactional(readOnly = true)
    public CertificateResponse getPublic(UUID publicId) {
        return CertificateResponse.from(publicCertificate(publicId), objectMapper);
    }

    @Transactional(readOnly = true)
    public Object getPublicPayload(UUID publicId) {
        return CertificateResponse.from(publicCertificate(publicId), objectMapper).payload();
    }

    private CanonicalCertificate canonical(UUID userId, UUID analysisId, String walletAddress) {
        return canonical(userId, ownedAnalysis(userId, analysisId), walletAddress);
    }

    private CanonicalCertificate canonical(UUID userId, ContributionAnalysis analysis, String walletAddress) {
        GitHubAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub account not found"));
        return payloadFactory.create(analysis, account, walletAddress);
    }

    private ContributionAnalysis ownedAnalysis(UUID userId, UUID analysisId) {
        return analysisRepository.findByIdAndSnapshotAnalysisJobUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));
    }

    private Certificate ownedCertificate(UUID userId, UUID id) {
        return certificateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
    }

    private Certificate publicCertificate(UUID publicId) {
        return certificateRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
    }
}
