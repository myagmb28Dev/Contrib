package com.example.project.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class AccessTokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsAccessToken() {
        AccessTokenCipher cipher = new AccessTokenCipher(KEY, new SecureRandom());

        String encrypted = cipher.encrypt("github-access-token");

        assertThat(encrypted).startsWith("v1:");
        assertThat(encrypted).doesNotContain("github-access-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("github-access-token");
    }

    @Test
    void usesUniqueInitializationVectorForEachEncryption() {
        AccessTokenCipher cipher = new AccessTokenCipher(KEY, new SecureRandom());

        String first = cipher.encrypt("same-token");
        String second = cipher.encrypt("same-token");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsKeyWithInvalidLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AccessTokenCipher(shortKey, new SecureRandom()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
