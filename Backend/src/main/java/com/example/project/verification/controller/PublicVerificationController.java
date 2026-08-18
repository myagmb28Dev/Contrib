package com.example.project.verification.controller;

import java.util.UUID;

import com.example.project.certificate.dto.CertificateResponse;
import com.example.project.certificate.service.CertificateService;
import com.example.project.verification.dto.VerificationResponse;
import com.example.project.verification.service.VerificationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/public/certificates")
public class PublicVerificationController {

    private final CertificateService certificateService;
    private final VerificationService verificationService;

    public PublicVerificationController(CertificateService certificateService,
            VerificationService verificationService) {
        this.certificateService = certificateService;
        this.verificationService = verificationService;
    }

    @GetMapping("/{publicId}")
    public CertificateResponse get(@PathVariable UUID publicId) {
        return certificateService.getPublic(publicId);
    }

    @GetMapping("/{publicId}/payload")
    public ResponseEntity<Object> payload(@PathVariable UUID publicId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=certificate-" + publicId + ".json")
                .body(certificateService.getPublicPayload(publicId));
    }

    @GetMapping("/{publicId}/verification")
    public VerificationResponse verify(@PathVariable UUID publicId) {
        return verificationService.verify(publicId);
    }
}
