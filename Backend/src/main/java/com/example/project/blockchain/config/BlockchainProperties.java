package com.example.project.blockchain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.blockchain")
public class BlockchainProperties {
    private long chainId;
    private String network;
    private String contractAddress;
    private String rpcUrl;

    public long getChainId() { return chainId; }
    public void setChainId(long chainId) { this.chainId = chainId; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getContractAddress() { return contractAddress; }
    public void setContractAddress(String contractAddress) { this.contractAddress = contractAddress; }
    public String getRpcUrl() { return rpcUrl; }
    public void setRpcUrl(String rpcUrl) { this.rpcUrl = rpcUrl; }

    public boolean isConfigured() {
        return contractAddress != null && contractAddress.matches("(?i)0x[0-9a-f]{40}")
                && rpcUrl != null && !rpcUrl.isBlank();
    }
}
