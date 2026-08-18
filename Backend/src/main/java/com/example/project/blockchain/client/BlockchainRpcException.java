package com.example.project.blockchain.client;

public class BlockchainRpcException extends RuntimeException {
    public BlockchainRpcException(String message) { super(message); }
    public BlockchainRpcException(String message, Throwable cause) { super(message, cause); }
}
