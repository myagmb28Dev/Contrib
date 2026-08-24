package com.example.project.blockchain.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.example.project.blockchain.domain.BlockchainAttestation;
import com.example.project.blockchain.domain.AttestationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockchainAttestationRepository extends JpaRepository<BlockchainAttestation, UUID> {
    Optional<BlockchainAttestation> findByCertificateId(UUID certificateId);
    Optional<BlockchainAttestation> findByTransactionHash(String transactionHash);
    List<BlockchainAttestation> findTop20ByStatusOrRevocationStatusOrderByUpdatedAtAsc(
            AttestationStatus status, AttestationStatus revocationStatus);
}
