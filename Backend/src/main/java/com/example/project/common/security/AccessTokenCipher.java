package com.example.project.common.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenCipher {

    private static final String VERSION_PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom;

    @Autowired
    public AccessTokenCipher(@Value("${app.security.token-encryption-key}") String encodedKey) {
        this(encodedKey, new SecureRandom());
    }

    AccessTokenCipher(String encodedKey, SecureRandom secureRandom) {
        this.key = decodeKey(encodedKey);
        this.secureRandom = secureRandom;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(iv.length + ciphertext.length);
            payload.put(iv);
            payload.put(ciphertext);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt GitHub access token", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || !encryptedValue.startsWith(VERSION_PREFIX)) {
            throw new IllegalArgumentException("Unsupported encrypted token format");
        }

        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encryptedValue.substring(VERSION_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Encrypted token payload is not valid Base64", exception);
        }

        if (payload.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted token payload is too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt GitHub access token", exception);
        }
    }

    private static SecretKey decodeKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must be configured");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must be valid Base64", exception);
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must decode to 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
