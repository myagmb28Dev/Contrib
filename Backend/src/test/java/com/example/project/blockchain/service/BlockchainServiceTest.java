package com.example.project.blockchain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.example.project.blockchain.client.EthereumJsonRpcClient;
import com.example.project.blockchain.config.BlockchainProperties;
import com.example.project.blockchain.domain.AttestationStatus;
import com.example.project.blockchain.domain.BlockchainAttestation;
import com.example.project.blockchain.dto.TransactionReceiptData;
import com.example.project.blockchain.repository.BlockchainAttestationRepository;
import com.example.project.certificate.domain.Certificate;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.example.project.certificate.payload.CertificatePayloadFactory;
import com.example.project.certificate.repository.CertificateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockchainServiceTest {

    private static final String CONTRACT = "0x1111111111111111111111111111111111111111";
    private static final String ISSUER = "0x2222222222222222222222222222222222222222";
    private static final String SUBJECT = "0x3333333333333333333333333333333333333333";
    private static final String CERTIFICATE_HASH = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String ONCHAIN_ID = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String TX_HASH = "0xcccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    private final EthereumJsonRpcClient rpcClient = mock(EthereumJsonRpcClient.class);
    private final EthereumKeccak256 hasher = new EthereumKeccak256();
    private BlockchainService service;
    private BlockchainAttestation attestation;
    private Certificate certificate;

    @BeforeEach
    void setUp() {
        BlockchainProperties properties = new BlockchainProperties();
        properties.setChainId(84532L);
        properties.setNetwork("base-sepolia");
        properties.setContractAddress(CONTRACT);
        properties.setRpcUrl("http://localhost:8545");
        service = new BlockchainService(properties, mock(CertificateRepository.class),
                mock(BlockchainAttestationRepository.class), mock(CertificatePayloadFactory.class),
                hasher, rpcClient);

        certificate = mock(Certificate.class);
        when(certificate.getCertificateHash()).thenReturn(CERTIFICATE_HASH);
        when(certificate.getIssuerWalletAddress()).thenReturn(ISSUER);
        when(certificate.getSubjectWalletAddress()).thenReturn(SUBJECT);
        attestation = BlockchainAttestation.pending(certificate, 84532L, "base-sepolia",
                CONTRACT, ONCHAIN_ID, TX_HASH);
    }

    @Test
    void confirmsReceiptContainingExactIssueEvent() throws Exception {
        when(rpcClient.getTransactionReceipt(TX_HASH)).thenReturn(receipt(CERTIFICATE_HASH));

        assertThat(service.refresh(attestation)).isTrue();
        assertThat(attestation.getStatus()).isEqualTo(AttestationStatus.CONFIRMED);
        assertThat(attestation.getBlockNumber()).isEqualTo(42L);
    }

    @Test
    void rejectsReceiptWhoseOnchainHashDoesNotMatch() throws Exception {
        String otherHash = "0xdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";
        when(rpcClient.getTransactionReceipt(TX_HASH)).thenReturn(receipt(otherHash));

        assertThat(service.refresh(attestation)).isFalse();
        assertThat(attestation.getStatus()).isEqualTo(AttestationStatus.FAILED);
    }

    @Test
    void confirmsRevocationReceiptAndRevokesCertificate() throws Exception {
        String revocationHash = "0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
        attestation.submitRevocation(revocationHash, "Superseded certificate");
        when(rpcClient.getTransactionReceipt(revocationHash)).thenReturn(revocationReceipt(revocationHash));

        assertThat(service.refreshRevocation(attestation)).isTrue();
        assertThat(attestation.getRevocationStatus()).isEqualTo(AttestationStatus.CONFIRMED);
        assertThat(attestation.getRevocationBlockNumber()).isEqualTo(43L);
        verify(certificate).revoke("Superseded certificate");
    }

    private TransactionReceiptData receipt(String emittedHash) throws Exception {
        String eventTopic = hasher.hashUtf8("CertificateIssued(bytes32,bytes32,address,address)");
        String issuerTopic = "0x" + "0".repeat(24) + ISSUER.substring(2);
        String subjectTopic = "0x" + "0".repeat(24) + SUBJECT.substring(2);
        String logs = """
                [{
                  "address":"%s",
                  "topics":["%s","%s","%s","%s"],
                  "data":"%s"
                }]
                """.formatted(CONTRACT, eventTopic, ONCHAIN_ID, issuerTopic, subjectTopic, emittedHash);
        return new TransactionReceiptData(TX_HASH, CONTRACT, true, 42L,
                new ObjectMapper().readTree(logs));
    }

    private TransactionReceiptData revocationReceipt(String transactionHash) throws Exception {
        String eventTopic = hasher.hashUtf8("CertificateRevoked(bytes32,address,uint64)");
        String issuerTopic = "0x" + "0".repeat(24) + ISSUER.substring(2);
        String logs = """
                [{
                  "address":"%s",
                  "topics":["%s","%s","%s"],
                  "data":"0x%s"
                }]
                """.formatted(CONTRACT, eventTopic, ONCHAIN_ID, issuerTopic, "0".repeat(63) + "1");
        return new TransactionReceiptData(transactionHash, CONTRACT, true, 43L,
                new ObjectMapper().readTree(logs));
    }
}
