package com.example.project.blockchain.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.example.project.blockchain.config.BlockchainProperties;
import com.example.project.blockchain.dto.OnchainAttestationData;
import com.example.project.blockchain.dto.TransactionReceiptData;
import com.example.project.certificate.hashing.EthereumKeccak256;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EthereumJsonRpcClientTest {

    private BlockchainProperties properties;
    private EthereumKeccak256 hasher;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new BlockchainProperties();
        properties.setRpcUrl("https://primary-rpc.example.com");
        properties.setFallbackRpcUrls(List.of("https://fallback-rpc-1.example.com", "https://fallback-rpc-2.example.com"));
        hasher = new EthereumKeccak256();
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void queriesPrimaryRpcWhenAvailable() {
        EthereumJsonRpcClient client = new EthereumJsonRpcClient(builder, properties, hasher);

        String sampleResult = "0x" + "0".repeat(63) + "1"
                + "0".repeat(24) + "1".repeat(40)
                + "0".repeat(24) + "2".repeat(40)
                + "0".repeat(63) + "5"
                + "0".repeat(63) + "9";

        server.expect(requestTo("https://primary-rpc.example.com"))
                .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"" + sampleResult + "\"}",
                        MediaType.APPLICATION_JSON));

        OnchainAttestationData data = client.getAttestation("0x" + "3".repeat(40), "0x" + "4".repeat(64));
        assertThat(data).isNotNull();
        assertThat(data.revokedAt()).isEqualTo(9L);
        server.verify();
    }

    @Test
    void fallsBackToSecondaryRpcWhenPrimaryFails() {
        EthereumJsonRpcClient client = new EthereumJsonRpcClient(builder, properties, hasher);

        String sampleResult = "0x" + "0".repeat(63) + "1"
                + "0".repeat(24) + "1".repeat(40)
                + "0".repeat(24) + "2".repeat(40)
                + "0".repeat(63) + "5"
                + "0".repeat(63) + "0";

        server.expect(requestTo("https://primary-rpc.example.com"))
                .andRespond(withServerError());

        server.expect(requestTo("https://fallback-rpc-1.example.com"))
                .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"" + sampleResult + "\"}",
                        MediaType.APPLICATION_JSON));

        OnchainAttestationData data = client.getAttestation("0x" + "3".repeat(40), "0x" + "4".repeat(64));
        assertThat(data).isNotNull();
        assertThat(data.issuedAt()).isEqualTo(5L);
        server.verify();
    }

    @Test
    void throwsExceptionWhenAllRpcEndpointsFail() {
        EthereumJsonRpcClient client = new EthereumJsonRpcClient(builder, properties, hasher);

        server.expect(requestTo("https://primary-rpc.example.com"))
                .andRespond(withServerError());
        server.expect(requestTo("https://fallback-rpc-1.example.com"))
                .andRespond(withServerError());
        server.expect(requestTo("https://fallback-rpc-2.example.com"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getAttestation("0x" + "3".repeat(40), "0x" + "4".repeat(64)))
                .isInstanceOf(BlockchainRpcException.class)
                .hasMessageContaining("Could not reach any blockchain RPC endpoints");
        server.verify();
    }

    @Test
    void fallsBackForTransactionReceiptQuery() {
        EthereumJsonRpcClient client = new EthereumJsonRpcClient(builder, properties, hasher);

        server.expect(requestTo("https://primary-rpc.example.com"))
                .andRespond(withServerError());

        server.expect(requestTo("https://fallback-rpc-1.example.com"))
                .andRespond(withSuccess("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "transactionHash": "0x123",
                            "to": "0x456",
                            "status": "0x1",
                            "blockNumber": "0x2a",
                            "logs": []
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TransactionReceiptData receipt = client.getTransactionReceipt("0x123");
        assertThat(receipt).isNotNull();
        assertThat(receipt.transactionHash()).isEqualTo("0x123");
        assertThat(receipt.successful()).isTrue();
        assertThat(receipt.blockNumber()).isEqualTo(42L);
        server.verify();
    }
}
