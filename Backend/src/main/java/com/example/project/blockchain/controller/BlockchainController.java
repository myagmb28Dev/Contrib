package com.example.project.blockchain.controller;

import java.util.UUID;

import com.example.project.auth.service.GitHubPrincipal;
import com.example.project.blockchain.dto.AttestationIntentResponse;
import com.example.project.blockchain.dto.AttestationResponse;
import com.example.project.blockchain.dto.SubmitTransactionRequest;
import com.example.project.blockchain.dto.SubmitRevocationRequest;
import com.example.project.blockchain.service.BlockchainService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BlockchainController {

    private final BlockchainService blockchainService;

    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @PostMapping("/certificates/{certificateId}/attestation-intent")
    public AttestationIntentResponse intent(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId) {
        return blockchainService.intent(principal.getUserId(), certificateId);
    }

    @PostMapping("/certificates/{certificateId}/transactions")
    public AttestationResponse submit(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId, @Valid @RequestBody SubmitTransactionRequest request) {
        return blockchainService.submit(principal.getUserId(), certificateId,
                request.transactionHash(), request.issuerWalletAddress());
    }

    @GetMapping("/certificates/{certificateId}/attestation")
    public AttestationResponse getForCertificate(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId) {
        return blockchainService.getForCertificate(principal.getUserId(), certificateId);
    }

    @PostMapping("/certificates/{certificateId}/revocation-intent")
    public AttestationIntentResponse revocationIntent(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId) {
        return blockchainService.revocationIntent(principal.getUserId(), certificateId);
    }

    @PostMapping("/certificates/{certificateId}/revocation-transactions")
    public AttestationResponse submitRevocation(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable UUID certificateId, @Valid @RequestBody SubmitRevocationRequest request) {
        return blockchainService.submitRevocation(principal.getUserId(), certificateId,
                request.transactionHash(), request.issuerWalletAddress(), request.reason());
    }

    @GetMapping("/blockchain/transactions/{transactionHash}")
    public AttestationResponse get(@AuthenticationPrincipal GitHubPrincipal principal,
            @PathVariable String transactionHash) {
        return blockchainService.get(principal.getUserId(), transactionHash);
    }
}
