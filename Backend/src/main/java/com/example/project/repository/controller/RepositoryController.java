package com.example.project.repository.controller;

import java.util.List;
import java.util.UUID;

import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.repository.dto.RepositoryResponse;
import com.example.project.repository.service.RepositoryService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping("/sync")
    public List<RepositoryResponse> synchronize(@AuthenticationPrincipal GitHubPrincipal principal) {
        return repositoryService.synchronize(principal.getUserId());
    }

    @GetMapping
    public List<RepositoryResponse> list(@AuthenticationPrincipal GitHubPrincipal principal) {
        return repositoryService.list(principal.getUserId());
    }

    @GetMapping("/{repositoryId}")
    public RepositoryResponse get(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID repositoryId) {
        return repositoryService.get(principal.getUserId(), repositoryId);
    }
}
