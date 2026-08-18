package com.example.project.certificate.controller;

import java.util.List;
import java.util.UUID;

import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.certificate.dto.CertificatePreviewResponse;
import com.example.project.certificate.dto.CertificateRequest;
import com.example.project.certificate.dto.CertificateResponse;
import com.example.project.certificate.dto.RevokeCertificateRequest;
import com.example.project.certificate.service.CertificateService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/preview")
    public CertificatePreviewResponse preview(@AuthenticationPrincipal GitHubPrincipal principal,
            @Valid @RequestBody CertificateRequest request) {
        return certificateService.preview(principal.getUserId(), request.analysisId(), request.subjectWalletAddress());
    }

    @PostMapping
    public CertificateResponse create(@AuthenticationPrincipal GitHubPrincipal principal,
            @Valid @RequestBody CertificateRequest request) {
        return certificateService.create(principal.getUserId(), request.analysisId(), request.subjectWalletAddress());
    }

    @GetMapping
    public List<CertificateResponse> list(@AuthenticationPrincipal GitHubPrincipal principal) {
        return certificateService.list(principal.getUserId());
    }

    @GetMapping("/{certificateId}")
    public CertificateResponse get(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId) {
        return certificateService.get(principal.getUserId(), certificateId);
    }

    @PostMapping("/{certificateId}/revoke")
    public CertificateResponse revoke(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId, @Valid @RequestBody RevokeCertificateRequest request) {
        return certificateService.revoke(principal.getUserId(), certificateId, request.reason());
    }
}
