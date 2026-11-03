package com.example.project.blockchain.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.project.blockchain.config.BlockchainProperties;
import com.example.project.blockchain.dto.TransactionReceiptData;
import com.example.project.blockchain.dto.OnchainAttestationData;
import com.example.project.certificate.hashing.EthereumKeccak256;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EthereumJsonRpcClient {

    private final RestClient restClient;
    private final BlockchainProperties properties;
    private final EthereumKeccak256 hasher;

    public EthereumJsonRpcClient(RestClient.Builder builder, BlockchainProperties properties,
            EthereumKeccak256 hasher) {
        this.restClient = builder.build();
        this.properties = properties;
        this.hasher = hasher;
    }

    public OnchainAttestationData getAttestation(String contractAddress, String certificateId) {
        String selector = hasher.hashUtf8("get(bytes32)").substring(2, 10);
        JsonNode result = rpc("eth_call", List.of(Map.of(
                "to", contractAddress,
                "data", "0x" + selector + certificateId.substring(2)), "latest"));
        String encoded = result.asText();
        if (!encoded.matches("0x[0-9a-fA-F]{320}")) {
            throw new BlockchainRpcException("Contract returned an invalid attestation payload");
        }
        String words = encoded.substring(2);
        return new OnchainAttestationData(
                "0x" + word(words, 0),
                "0x" + word(words, 1).substring(24),
                "0x" + word(words, 2).substring(24),
                parseWord(word(words, 3)),
                parseWord(word(words, 4)));
    }

    private JsonNode rpc(String method, List<?> params) {
        List<String> urls = resolveRpcUrls();
        if (urls.isEmpty()) {
            throw new BlockchainRpcException("Blockchain RPC URL is not configured");
        }
        Exception lastException = null;
        for (String url : urls) {
            try {
                JsonNode response = restClient.post().uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("jsonrpc", "2.0", "id", 1, "method", method, "params", params))
                        .retrieve().body(JsonNode.class);
                if (response == null || response.hasNonNull("error") || !response.has("result")) {
                    throw new BlockchainRpcException("Blockchain RPC returned an error from " + url);
                }
                return response.get("result");
            } catch (Exception exception) {
                lastException = exception;
            }
        }
        if (lastException instanceof BlockchainRpcException rpcException) {
            throw rpcException;
        }
        throw new BlockchainRpcException("Could not reach any blockchain RPC endpoints", lastException);
    }

    public TransactionReceiptData getTransactionReceipt(String transactionHash) {
        List<String> urls = resolveRpcUrls();
        if (urls.isEmpty()) {
            throw new BlockchainRpcException("Blockchain RPC URL is not configured");
        }
        Exception lastException = null;
        for (String url : urls) {
            try {
                JsonNode response = restClient.post().uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("jsonrpc", "2.0", "id", 1, "method", "eth_getTransactionReceipt",
                                "params", List.of(transactionHash)))
                        .retrieve().body(JsonNode.class);
                if (response == null || response.hasNonNull("error")) {
                    throw new BlockchainRpcException("Blockchain RPC returned an error from " + url);
                }
                JsonNode result = response.get("result");
                if (result == null || result.isNull()) {
                    return null;
                }
                return new TransactionReceiptData(
                        result.path("transactionHash").asText(),
                        result.path("to").asText(),
                        "0x1".equals(result.path("status").asText()),
                        parseHexLong(result.path("blockNumber").asText()),
                        result.path("logs"));
            } catch (Exception exception) {
                lastException = exception;
            }
        }
        if (lastException instanceof BlockchainRpcException rpcException) {
            throw rpcException;
        }
        throw new BlockchainRpcException("Could not reach any blockchain RPC endpoints", lastException);
    }

    private List<String> resolveRpcUrls() {
        List<String> urls = new ArrayList<>();
        if (properties.getRpcUrl() != null && !properties.getRpcUrl().isBlank()) {
            urls.add(properties.getRpcUrl().trim());
        }
        if (properties.getFallbackRpcUrls() != null) {
            for (String fallback : properties.getFallbackRpcUrls()) {
                if (fallback != null && !fallback.isBlank() && !urls.contains(fallback.trim())) {
                    urls.add(fallback.trim());
                }
            }
        }
        return urls;
    }

    private long parseHexLong(String value) {
        if (value == null || value.isBlank() || "0x".equals(value)) {
            return 0;
        }
        return Long.parseUnsignedLong(value.substring(2), 16);
    }

    private String word(String encoded, int index) {
        return encoded.substring(index * 64, (index + 1) * 64);
    }

    private long parseWord(String word) {
        return Long.parseUnsignedLong(word.substring(48), 16);
    }
}
