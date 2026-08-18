package com.example.project.analysis.controller;

import java.util.List;
import java.util.UUID;

import com.example.project.analysis.dto.AnalysisJobResponse;
import com.example.project.analysis.dto.AnalysisResponse;
import com.example.project.analysis.dto.CreateAnalysisRequest;
import com.example.project.analysis.service.AnalysisService;
import com.example.project.auth.service.GitHubPrincipal;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/repositories/{repositoryId}/analyses")
    public ResponseEntity<AnalysisJobResponse> create(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID repositoryId, @Valid @RequestBody CreateAnalysisRequest request) {
        return ResponseEntity.accepted().body(analysisService.create(principal.getUserId(), repositoryId,
                request.periodStart(), request.periodEnd()));
    }

    @GetMapping("/analysis-jobs/{jobId}")
    public AnalysisJobResponse getJob(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID jobId) {
        return analysisService.getJob(principal.getUserId(), jobId);
    }

    @PostMapping("/analysis-jobs/{jobId}/retry")
    public ResponseEntity<AnalysisJobResponse> retry(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID jobId) {
        return ResponseEntity.accepted().body(analysisService.retry(principal.getUserId(), jobId));
    }

    @GetMapping("/analyses/{analysisId}")
    public AnalysisResponse getAnalysis(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID analysisId) {
        return analysisService.getAnalysis(principal.getUserId(), analysisId);
    }

    @PostMapping("/analyses/{analysisId}/ai-summary")
    public AnalysisResponse regenerateSummary(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID analysisId) {
        return analysisService.regenerateSummary(principal.getUserId(), analysisId);
    }

    @GetMapping("/analyses")
    public List<AnalysisResponse> list(@AuthenticationPrincipal GitHubPrincipal principal) {
        return analysisService.list(principal.getUserId());
    }

    @GetMapping("/repositories/{repositoryId}/analyses")
    public List<AnalysisResponse> list(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID repositoryId) {
        return analysisService.listForRepository(principal.getUserId(), repositoryId);
    }
}
