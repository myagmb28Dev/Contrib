package com.example.project.certificate.hashing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EthereumKeccak256Test {

    private final EthereumKeccak256 hasher = new EthereumKeccak256();

    @Test
    void matchesEthereumKeccak256EmptyStringVector() {
        assertThat(hasher.hashUtf8("")).isEqualTo(
                "0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");
    }

    @Test
    void canonicalJsonHasStableHash() {
        assertThat(hasher.hashUtf8("{\"schemaVersion\":\"1.0\",\"score\":42}"))
                .isEqualTo("0x1f8ca027a1b9bc0e7bb8e46e87a262221e31067cdaef1f8996729f99caf93b5e");
    }
}
