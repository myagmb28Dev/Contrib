package com.example.project.blockchain.service;

import com.example.project.blockchain.domain.AttestationStatus;
import com.example.project.blockchain.repository.BlockchainAttestationRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "app.blockchain.receipt-poll-enabled", havingValue = "true", matchIfMissing = true)
public class BlockchainReceiptReconciler {

    private final BlockchainAttestationRepository repository;
    private final BlockchainService blockchainService;

    public BlockchainReceiptReconciler(BlockchainAttestationRepository repository,
            BlockchainService blockchainService) {
        this.repository = repository;
        this.blockchainService = blockchainService;
    }

    @Scheduled(fixedDelayString = "${app.blockchain.receipt-poll-interval-ms:3000}")
    public void reconcile() {
        repository.findTop20ByStatusOrRevocationStatusOrderByUpdatedAtAsc(
                AttestationStatus.PENDING, AttestationStatus.PENDING)
                .forEach(attestation -> blockchainService.refreshPending(attestation.getId()));
    }
}
