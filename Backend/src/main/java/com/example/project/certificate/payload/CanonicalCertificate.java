package com.example.project.certificate.payload;

import java.time.Instant;
import java.util.Map;

public record CanonicalCertificate(String json, String hash, Map<String, Object> payload, Instant issuedAt) {
}
