package com.example.project.certificate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.project.certificate.domain.Certificate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    Optional<Certificate> findByIdAndUserId(UUID id, UUID userId);
    Optional<Certificate> findByPublicId(UUID publicId);
    Optional<Certificate> findByAnalysisId(UUID analysisId);
    List<Certificate> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
