package com.example.project.analysis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.UUID;
import com.example.project.analysis.service.AnalysisService;
import com.example.project.auth.service.GitHubPrincipal;
import org.junit.jupiter.api.Test;

class AnalysisControllerTest {
    @Test void newAnalysisAlwaysUsesServerResolvedFullHistory() {
        var service = mock(AnalysisService.class);
        var principal = mock(GitHubPrincipal.class);
        var userId = UUID.randomUUID();
        var repositoryId = UUID.randomUUID();
        when(principal.getUserId()).thenReturn(userId);
        var response = new AnalysisController(service).create(principal, repositoryId);
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(service).create(userId, repositoryId, null, null, true);
    }
}
