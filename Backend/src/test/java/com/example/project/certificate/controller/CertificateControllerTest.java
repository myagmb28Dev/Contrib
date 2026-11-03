package com.example.project.certificate.controller;

import static org.mockito.Mockito.*;

import java.util.UUID;

import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.certificate.service.CertificateService;

import org.junit.jupiter.api.Test;

class CertificateControllerTest {

    @Test
    void delegatesDeletionToService() {
        CertificateService service = mock(CertificateService.class);
        GitHubPrincipal principal = mock(GitHubPrincipal.class);
        UUID userId = UUID.randomUUID();
        UUID certificateId = UUID.randomUUID();
        when(principal.getUserId()).thenReturn(userId);

        new CertificateController(service).delete(principal, certificateId);

        verify(service).delete(userId, certificateId);
    }
}
