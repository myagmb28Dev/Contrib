package com.example.project.certificate.hashing;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.springframework.stereotype.Component;

@Component
public class EthereumKeccak256 {

    public String hashUtf8(String value) {
        Keccak.Digest256 digest = new Keccak.Digest256();
        byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return "0x" + HexFormat.of().formatHex(hashed);
    }
}
